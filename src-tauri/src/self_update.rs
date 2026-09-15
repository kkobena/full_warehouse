//! Mise à jour autonome du poste client.
//!
//! # Pourquoi un mécanisme maison plutôt que `tauri-plugin-updater`
//!
//! Sur les postes clients, PharmaSmart est déployé par simple dépôt de
//! `pharmasmart.exe` à côté d'un `backend-url.txt` — aucune installation. Or le
//! plugin officiel télécharge et **exécute un installeur** (`-setup.exe` / `.msi`)
//! et ne sait pas remplacer un exécutable autonome. Il ne s'applique donc pas ici.
//!
//! Ce module automatise exactement ce qui se fait aujourd'hui à la main : récupérer
//! le nouvel exécutable auprès du poste serveur et remplacer celui du poste.
//!
//! # Pas de signature — décision assumée
//!
//! Le binaire est récupéré sur le LAN de l'officine, auprès de son propre serveur,
//! par un poste qui lui parle déjà pour tout le reste. Le niveau de confiance est
//! donc celui de la copie manuelle qu'il remplace, ni plus ni moins. Ajouter une
//! chaîne de signature imposerait une gestion de clés (conservation, sauvegarde,
//! rotation) sans rien changer au modèle de menace réel.
//!
//! Le filet retenu est plus simple et suffisant : l'exécutable précédent est
//! conservé en `.old`, ce qui permet de revenir en arrière si la nouvelle version
//! pose problème.

use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};

/// Suffixe du binaire mis de côté avant bascule, conservé pour retour arrière.
const BACKUP_SUFFIX: &str = ".old";

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct UpdateStatus {
    /// `true` si une version différente est disponible sur le poste serveur.
    pub available: bool,
    /// Version en cours d'exécution sur ce poste.
    pub current_version: String,
    /// Version proposée par le serveur, si disponible.
    pub new_version: Option<String>,
    /// Message destiné à l'opérateur.
    pub message: String,
}

/// Manifeste renvoyé par `/api/updates/{target}/{arch}/{version}`.
#[derive(Debug, Deserialize)]
struct UpdateManifest {
    version: String,
    url: String,
}

#[derive(Debug, thiserror::Error)]
pub enum UpdateError {
    #[error("Serveur injoignable : {0}")]
    ServerUnreachable(String),

    #[error("Réponse inattendue du serveur ({0})")]
    UnexpectedResponse(String),

    #[error("Téléchargement interrompu : {0}")]
    DownloadFailed(String),

    #[error("Emplacement de l'application introuvable : {0}")]
    ExecutableNotFound(String),

    #[error("Bascule impossible : {0}")]
    SwapFailed(String),
}

impl From<UpdateError> for String {
    fn from(e: UpdateError) -> Self {
        e.to_string()
    }
}

/// Construit l'URL du relais à partir de l'URL backend déjà résolue par le poste.
///
/// L'endpoint ne peut pas être figé à la compilation : chaque officine a une adresse
/// de serveur différente, et un binaire unique doit servir partout. On le dérive donc
/// de l'adresse à laquelle ce poste parle déjà à son backend.
fn check_url(backend_url: &str, current_version: &str) -> String {
    format!(
        "{}/api/updates/windows/x86_64/{}",
        backend_url.trim_end_matches('/'),
        current_version
    )
}

/// Interroge le poste serveur. Ne télécharge rien.
pub async fn check_for_update(
    client: &reqwest::Client,
    backend_url: &str,
    current_version: &str,
) -> UpdateStatus {
    let url = check_url(backend_url, current_version);
    match client.get(&url).send().await {
        // 204 : le serveur n'a rien à proposer. Cas nominal, pas une anomalie.
        Ok(r) if r.status() == reqwest::StatusCode::NO_CONTENT => UpdateStatus {
            available: false,
            current_version: current_version.to_string(),
            new_version: None,
            message: "Ce poste est à jour.".to_string(),
        },
        Ok(r) if r.status().is_success() => match r.json::<UpdateManifest>().await {
            Ok(manifest) => UpdateStatus {
                available: true,
                current_version: current_version.to_string(),
                new_version: Some(manifest.version.clone()),
                message: format!("Version {} disponible sur le serveur.", manifest.version),
            },
            Err(e) => UpdateStatus {
                available: false,
                current_version: current_version.to_string(),
                new_version: None,
                message: format!("Manifeste illisible : {e}"),
            },
        },
        Ok(r) => UpdateStatus {
            available: false,
            current_version: current_version.to_string(),
            new_version: None,
            message: format!("Réponse inattendue du serveur ({})", r.status()),
        },
        Err(e) => UpdateStatus {
            available: false,
            current_version: current_version.to_string(),
            new_version: None,
            message: format!("Serveur injoignable : {e}"),
        },
    }
}

/// Télécharge la nouvelle version et remplace l'exécutable.
/// Retourne le chemin du binaire en place, prêt à être relancé.
pub async fn download_and_apply(
    client: &reqwest::Client,
    backend_url: &str,
    current_version: &str,
) -> Result<PathBuf, UpdateError> {
    let url = check_url(backend_url, current_version);
    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| UpdateError::ServerUnreachable(e.to_string()))?;

    if response.status() == reqwest::StatusCode::NO_CONTENT {
        return Err(UpdateError::UnexpectedResponse(
            "le serveur ne propose aucune mise à jour".to_string(),
        ));
    }
    if !response.status().is_success() {
        return Err(UpdateError::UnexpectedResponse(response.status().to_string()));
    }

    let manifest: UpdateManifest = response
        .json()
        .await
        .map_err(|e| UpdateError::UnexpectedResponse(e.to_string()))?;

    let binary = client
        .get(&manifest.url)
        .send()
        .await
        .map_err(|e| UpdateError::DownloadFailed(e.to_string()))?
        .error_for_status()
        .map_err(|e| UpdateError::DownloadFailed(e.to_string()))?
        .bytes()
        .await
        .map_err(|e| UpdateError::DownloadFailed(e.to_string()))?;

    // Un téléchargement tronqué produirait un exécutable invalide. Le poste ne
    // redémarrerait plus, et il faudrait intervenir physiquement dessus : mieux vaut
    // refuser un binaire manifestement incomplet.
    if binary.len() < 1_000_000 {
        return Err(UpdateError::DownloadFailed(format!(
            "fichier anormalement petit ({} octets) — téléchargement probablement interrompu",
            binary.len()
        )));
    }

    tracing::info!(
        version = %manifest.version,
        bytes = binary.len(),
        "Téléchargement terminé — bascule de l'exécutable"
    );

    swap_executable(&binary)
}

/// Remplace l'exécutable courant.
///
/// Windows interdit de supprimer un exécutable en cours d'exécution, mais **autorise
/// à le renommer** : on met donc l'ancien de côté, on écrit le nouveau à sa place, et
/// le prochain démarrage prend la nouvelle version. Aucun processus tiers n'est
/// nécessaire.
///
/// Le `.old` n'est pas un résidu : c'est le moyen de revenir en arrière si la nouvelle
/// version pose problème, en le renommant.
fn swap_executable(new_binary: &[u8]) -> Result<PathBuf, UpdateError> {
    let current = std::env::current_exe()
        .map_err(|e| UpdateError::ExecutableNotFound(e.to_string()))?;
    let backup = backup_path(&current);

    // Une sauvegarde d'une mise à jour précédente empêcherait le renommage.
    if backup.exists() {
        std::fs::remove_file(&backup).map_err(|e| {
            UpdateError::SwapFailed(format!("ancienne sauvegarde {backup:?} non supprimable : {e}"))
        })?;
    }

    std::fs::rename(&current, &backup)
        .map_err(|e| UpdateError::SwapFailed(format!("mise de côté de l'exécutable : {e}")))?;

    // À partir d'ici, l'emplacement nominal est vide : en cas d'échec d'écriture, on
    // remet impérativement l'ancien binaire, faute de quoi le poste n'aurait plus
    // d'application du tout.
    if let Err(e) = std::fs::write(&current, new_binary) {
        let _ = std::fs::rename(&backup, &current);
        return Err(UpdateError::SwapFailed(format!(
            "écriture du nouvel exécutable : {e} (version précédente restaurée)"
        )));
    }

    Ok(current)
}

fn backup_path(current: &Path) -> PathBuf {
    let mut name = current
        .file_name()
        .map(|n| n.to_string_lossy().to_string())
        .unwrap_or_else(|| "pharmasmart.exe".to_string());
    name.push_str(BACKUP_SUFFIX);
    current.with_file_name(name)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn l_url_de_controle_derive_de_l_url_backend() {
        assert_eq!(
            check_url("http://192.168.1.10:9080", "1.4.2"),
            "http://192.168.1.10:9080/api/updates/windows/x86_64/1.4.2"
        );
    }

    #[test]
    fn l_url_de_controle_tolere_une_barre_finale() {
        assert_eq!(
            check_url("http://192.168.1.10:9080/", "1.4.2"),
            "http://192.168.1.10:9080/api/updates/windows/x86_64/1.4.2"
        );
    }

    #[test]
    fn la_sauvegarde_se_place_a_cote_de_l_executable() {
        let backup = backup_path(Path::new(r"C:\PharmaSmart\pharmasmart.exe"));
        assert_eq!(backup, PathBuf::from(r"C:\PharmaSmart\pharmasmart.exe.old"));
    }
}
