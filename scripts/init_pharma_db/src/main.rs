#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use eframe::egui;
use tokio_postgres::{Client, NoTls};
use std::sync::{Arc, Mutex};

const DEFAULT_PG_ADMIN_USER: &str = "postgres";
const DEFAULT_DB_NAME: &str = "pharma_smart";
const DEFAULT_USER: &str = "pharma_smart";

fn main() -> Result<(), eframe::Error> {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([600.0, 820.0])
            .with_resizable(false),
        ..Default::default()
    };

    eframe::run_native(
        "Initialisation Base de Données PharmaSmart",
        options,
        Box::new(|cc| {
            // Configuration du style personnalisé
            configure_custom_style(&cc.egui_ctx);
            Ok(Box::new(PharmaDbApp::default()))
        }),
    )
}

fn configure_custom_style(ctx: &egui::Context) {
    let mut style = (*ctx.style()).clone();
    
    // Taille de police augmentée
    style.text_styles.insert(
        egui::TextStyle::Body,
        egui::FontId::new(14.0, egui::FontFamily::Proportional),
    );
    style.text_styles.insert(
        egui::TextStyle::Button,
        egui::FontId::new(15.0, egui::FontFamily::Proportional),
    );
    style.text_styles.insert(
        egui::TextStyle::Heading,
        egui::FontId::new(20.0, egui::FontFamily::Proportional),
    );
    
    // Style des widgets
    style.spacing.item_spacing = egui::vec2(8.0, 12.0);
    style.spacing.button_padding = egui::vec2(16.0, 8.0);
    style.spacing.window_margin = egui::Margin::same(15.0);
    
    // Coins arrondis
    style.visuals.window_rounding = egui::Rounding::same(8.0);
    style.visuals.widgets.noninteractive.rounding = egui::Rounding::same(6.0);
    style.visuals.widgets.inactive.rounding = egui::Rounding::same(6.0);
    style.visuals.widgets.hovered.rounding = egui::Rounding::same(6.0);
    style.visuals.widgets.active.rounding = egui::Rounding::same(6.0);
    
    // Couleurs des champs de texte
    style.visuals.widgets.inactive.bg_fill = egui::Color32::from_rgb(245, 245, 245);
    style.visuals.widgets.inactive.weak_bg_fill = egui::Color32::from_rgb(240, 240, 240);
    style.visuals.widgets.inactive.bg_stroke = egui::Stroke::new(1.5, egui::Color32::from_rgb(200, 200, 200));
    
    style.visuals.widgets.hovered.bg_fill = egui::Color32::from_rgb(255, 255, 255);
    style.visuals.widgets.hovered.bg_stroke = egui::Stroke::new(2.0, egui::Color32::from_rgb(100, 150, 255));
    
    style.visuals.widgets.active.bg_fill = egui::Color32::from_rgb(255, 255, 255);
    style.visuals.widgets.active.bg_stroke = egui::Stroke::new(2.0, egui::Color32::from_rgb(70, 130, 255));
    
    ctx.set_style(style);
}

#[derive(Default)]
struct PharmaDbApp {
    db_name: String,
    db_user: String,
    pg_admin_user: String,
    pg_admin_pass: String,
    new_user_pass: String,
    status_message: Arc<Mutex<String>>,
    is_processing: Arc<Mutex<bool>>,
    success: Arc<Mutex<bool>>,
}

impl PharmaDbApp {
    fn get_db_name(&self) -> String {
        if self.db_name.is_empty() {
            DEFAULT_DB_NAME.to_string()
        } else {
            self.db_name.clone()
        }
    }

    fn get_db_user(&self) -> String {
        if self.db_user.is_empty() {
            DEFAULT_USER.to_string()
        } else {
            self.db_user.clone()
        }
    }
}

impl eframe::App for PharmaDbApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        egui::CentralPanel::default().show(ctx, |ui| {
            ui.add_space(8.0);
            
            // En-tête avec style
            ui.vertical_centered(|ui| {
                ui.heading(egui::RichText::new("🗄️ Initialisation Base de Données PharmaSmart")
                    .size(22.0)
                    .color(egui::Color32::from_rgb(60, 80, 140)));
                ui.add_space(3.0);
                ui.label(egui::RichText::new("Configuration automatique de Pharma Smart")
                    .size(13.0)
                    .color(egui::Color32::GRAY));
            });
            
            ui.add_space(11.0);

            // Configuration fixe avec style amélioré
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(240, 245, 255))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(200, 215, 240)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);
                    
                    ui.label(egui::RichText::new("📋 Configuration")
                        .size(16.0)
                        .strong()
                        .color(egui::Color32::from_rgb(60, 80, 140)));
                    ui.add_space(8.0);
                    
                    // Base de données
                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0], 
                            egui::Label::new(egui::RichText::new("Base de données:").size(13.0))
                        );
                        egui::Frame::none()
                            .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(180, 180, 180)))
                            .rounding(4.0)
                            .inner_margin(egui::Margin::symmetric(8.0, 4.0))
                            .show(ui, |ui| {
                                let response = ui.add_sized([354.0, 20.0],
                                    egui::TextEdit::singleline(&mut self.db_name)
                                        .hint_text("pharma_smart")
                                        .frame(false)
                                );
                                if response.lost_focus() && self.db_name.is_empty() {
                                    self.db_name = DEFAULT_DB_NAME.to_string();
                                }
                            });
                    });
                    
                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        let hint_text = if self.db_name.is_empty() {
                            format!("→ Valeur par défaut: {}", DEFAULT_DB_NAME)
                        } else {
                            String::from(" ")
                        };
                        ui.label(egui::RichText::new(hint_text)
                            .size(11.0)
                            .italics()
                            .color(egui::Color32::DARK_GRAY));
                    });
                    
                    ui.add_space(1.5);
                    
                    // Utilisateur
                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0], 
                            egui::Label::new(egui::RichText::new("Utilisateur:").size(13.0))
                        );
                        egui::Frame::none()
                            .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(180, 180, 180)))
                            .rounding(4.0)
                            .inner_margin(egui::Margin::symmetric(8.0, 4.0))
                            .show(ui, |ui| {
                                let response = ui.add_sized([354.0, 20.0],
                                    egui::TextEdit::singleline(&mut self.db_user)
                                        .hint_text("pharma_smart")
                                        .frame(false)
                                );
                                if response.lost_focus() && self.db_user.is_empty() {
                                    self.db_user = DEFAULT_USER.to_string();
                                }
                            });
                    });
                    
                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        let hint_text = if self.db_user.is_empty() {
                            format!("→ Valeur par défaut: {}", DEFAULT_USER)
                        } else {
                            String::from(" ")
                        };
                        ui.label(egui::RichText::new(hint_text)
                            .size(11.0)
                            .italics()
                            .color(egui::Color32::DARK_GRAY));
                    });
                    
                    ui.add_space(1.5);
                
                    // Schéma (lecture seule, basé sur le nom de la base)
                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0], 
                            egui::Label::new(egui::RichText::new("Schéma:").size(13.0))
                        );
                        let schema_name = self.get_db_name();
                        ui.label(egui::RichText::new(schema_name)
                            .size(14.0)
                            .strong()
                            .color(egui::Color32::from_rgb(70, 130, 180)));
                    });
                    
                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        ui.label(egui::RichText::new("→ Le schéma aura le même nom que la base")
                            .size(11.0)
                            .italics()
                            .color(egui::Color32::DARK_GRAY));
                    });
                    
                     ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Champs de saisie administrateur avec style amélioré
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(252, 248, 245))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(230, 215, 200)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);
                    
                    ui.label(egui::RichText::new("🔐 Identifiants Administrateur PostgreSQL")
                        .size(16.0)
                        .strong()
                        .color(egui::Color32::from_rgb(140, 80, 60)));
                    ui.add_space(10.0);

                    // Utilisateur admin
                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0], 
                            egui::Label::new(egui::RichText::new("Utilisateur admin:").size(13.0))
                        );
                        egui::Frame::none()
                            .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(180, 180, 180)))
                            .rounding(4.0)
                            .inner_margin(egui::Margin::symmetric(8.0, 4.0))
                            .show(ui, |ui| {
                                let response = ui.add_sized([354.0, 20.0],
                                    egui::TextEdit::singleline(&mut self.pg_admin_user)
                                        .hint_text("postgres")
                                        .frame(false)
                                );
                                if response.lost_focus() && self.pg_admin_user.is_empty() {
                                    self.pg_admin_user = DEFAULT_PG_ADMIN_USER.to_string();
                                }
                            });
                    });
                    
                    // Message d'aide fixe pour éviter le redimensionnement
                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        let hint_text = if self.pg_admin_user.is_empty() {
                            format!("→ Valeur par défaut: {}", DEFAULT_PG_ADMIN_USER)
                        } else {
                            String::from(" ")
                        };
                        ui.label(egui::RichText::new(hint_text)
                            .size(11.0)
                            .italics()
                            .color(egui::Color32::DARK_GRAY));
                    });

                     ui.add_space(1.5);

                    // Mot de passe admin
                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0],
                            egui::Label::new(egui::RichText::new("Mot de passe admin:").size(13.0))
                        );
                        egui::Frame::none()
                            .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(180, 180, 180)))
                            .rounding(4.0)
                            .inner_margin(egui::Margin::symmetric(8.0, 4.0))
                            .show(ui, |ui| {
                                ui.add_sized([354.0, 20.0],
                                    egui::TextEdit::singleline(&mut self.pg_admin_pass)
                                        .password(true)
                                        .hint_text("••••••••")
                                        .frame(false)
                                );
                            });
                    });
                    
                    // Espace fixe pour stabilité
                    ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Mot de passe du nouvel utilisateur avec style amélioré
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(245, 252, 245))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(200, 230, 200)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);
                    
                    ui.label(egui::RichText::new("🔑 Mot de passe du Nouvel Utilisateur")
                        .size(16.0)
                        .strong()
                        .color(egui::Color32::from_rgb(60, 120, 60)));
                    ui.add_space(10.0);

                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0],
                            egui::Label::new(egui::RichText::new("Mot de passe:").size(13.0))
                        );
                        egui::Frame::none()
                            .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(180, 180, 180)))
                            .rounding(4.0)
                            .inner_margin(egui::Margin::symmetric(8.0, 4.0))
                            .show(ui, |ui| {
                                ui.add_sized([354.0, 20.0],
                                    egui::TextEdit::singleline(&mut self.new_user_pass)
                                        .password(true)
                                        .hint_text("••••••••")
                                        .frame(false)
                                );
                            });
                    });
                    
                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        ui.label(egui::RichText::new("→ Sera utilisé pour l'utilisateur 'pharma_smart'")
                            .size(11.0)
                            .italics()
                            .color(egui::Color32::DARK_GRAY));
                    });
                    
                    // Espace fixe pour stabilité
                    ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Bouton de création avec style amélioré
            let is_processing = *self.is_processing.lock().unwrap();
            let can_submit = !self.pg_admin_pass.is_empty() 
                && !self.new_user_pass.is_empty() 
                && !is_processing;

            ui.vertical_centered(|ui| {
                let button_color = if can_submit {
                    egui::Color32::from_rgb(70, 130, 180)
                } else {
                    egui::Color32::from_rgb(180, 180, 180)
                };

                let button = egui::Button::new(
                    egui::RichText::new(if is_processing { "⏳ Traitement en cours..." } else { "✅ Créer la base de données" })
                        .size(16.0)
                        .color(egui::Color32::WHITE)
                )
                .fill(button_color)
                .min_size(egui::vec2(350.0, 45.0))
                .rounding(8.0);

                if ui.add_enabled(can_submit, button).clicked() {
                    let admin_user = if self.pg_admin_user.is_empty() {
                        DEFAULT_PG_ADMIN_USER.to_string()
                    } else {
                        self.pg_admin_user.clone()
                    };
                    let admin_pass = self.pg_admin_pass.clone();
                    let new_pass = self.new_user_pass.clone();
                    let db_name = self.get_db_name();
                    let db_user = self.get_db_user();
                    let status_message = Arc::clone(&self.status_message);
                    let is_processing = Arc::clone(&self.is_processing);
                    let success = Arc::clone(&self.success);
                    let ctx_clone = ctx.clone();

                    *is_processing.lock().unwrap() = true;
                    *status_message.lock().unwrap() = "Traitement en cours...".to_string();

                    std::thread::spawn(move || {
                        let rt = tokio::runtime::Runtime::new().unwrap();
                        let result = rt.block_on(async {
                            execute_database_setup(&admin_user, &admin_pass, &new_pass, &db_name, &db_user).await
                        });

                        match result {
                            Ok(_) => {
                                *status_message.lock().unwrap() = "✓ Base de données créée avec succès!".to_string();
                                *success.lock().unwrap() = true;
                            }
                            Err(e) => {
                                *status_message.lock().unwrap() = format!("✗ Erreur: {}", e);
                                *success.lock().unwrap() = false;
                            }
                        }
                        *is_processing.lock().unwrap() = false;
                        ctx_clone.request_repaint();
                    });
                }

                if is_processing {
                    ui.spinner();
                }
            });

            ui.add_space(10.0);

            // Message de statut avec style amélioré
            let status = self.status_message.lock().unwrap().clone();
            if !status.is_empty() {
                let success_state = *self.success.lock().unwrap();
                let (bg_color, text_color, border_color) = if status.contains("Traitement") {
                    (
                        egui::Color32::from_rgb(240, 240, 245),
                        egui::Color32::from_rgb(80, 80, 120),
                        egui::Color32::from_rgb(180, 180, 200)
                    )
                } else if success_state {
                    (
                        egui::Color32::from_rgb(230, 250, 230),
                        egui::Color32::from_rgb(40, 120, 40),
                        egui::Color32::from_rgb(150, 220, 150)
                    )
                } else {
                    (
                        egui::Color32::from_rgb(255, 235, 235),
                        egui::Color32::from_rgb(180, 40, 40),
                        egui::Color32::from_rgb(255, 150, 150)
                    )
                };

                egui::Frame::none()
                    .fill(bg_color)
                    .rounding(8.0)
                    .inner_margin(15.0)
                    .stroke(egui::Stroke::new(2.0, border_color))
                    .show(ui, |ui| {
                        ui.set_max_width(540.0);
                        ui.horizontal(|ui| {
                            if is_processing {
                                ui.spinner();
                                ui.add_space(10.0);
                            }
                            ui.label(egui::RichText::new(&status)
                                .size(14.0)
                                .strong()
                                .color(text_color));
                        });
                    });
            }

            ui.add_space(10.0);
        });
    }
}

async fn execute_database_setup(
    admin_user: &str,
    admin_pass: &str,
    new_pass: &str,
    db_name: &str,
    db_user: &str,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let new_schema = db_name; // Le schéma a le même nom que la base de données

    // Se connecter à PostgreSQL
    let connection_string = format!(
        "host=localhost user={} password={} dbname=postgres",
        admin_user, admin_pass
    );

    let (client, connection) = tokio_postgres::connect(&connection_string, NoTls).await?;

    tokio::spawn(async move {
        if let Err(e) = connection.await {
            eprintln!("Erreur de connexion: {}", e);
        }
    });

    // Créer l'utilisateur et la base de données
    create_user_and_database(&client, db_name, db_user, new_pass).await?;

    // Se reconnecter à la nouvelle base de données
    let new_connection_string = format!(
        "host=localhost user={} password={} dbname={}",
        admin_user, admin_pass, db_name
    );

    let (new_client, new_connection) = tokio_postgres::connect(&new_connection_string, NoTls).await?;

    tokio::spawn(async move {
        if let Err(e) = new_connection.await {
            eprintln!("Erreur de connexion: {}", e);
        }
    });

    // Créer le schéma et attribuer les privilèges
    create_schema_and_grant_privileges(&new_client, new_schema, db_user).await?;

    Ok(())
}

async fn create_user_and_database(
    client: &Client,
    db_name: &str,
    username: &str,
    password: &str,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Créer la base de données
    let create_db = format!("CREATE DATABASE {}", db_name);
    client.execute(&create_db, &[]).await?;

    // Créer l'utilisateur
    let create_user = format!("CREATE USER {} WITH PASSWORD '{}'", username, password);
    client.execute(&create_user, &[]).await?;

    // Changer le propriétaire de la base de données
    let alter_db = format!("ALTER DATABASE {} OWNER TO {}", db_name, username);
    client.execute(&alter_db, &[]).await?;

    Ok(())
}

async fn create_schema_and_grant_privileges(
    client: &Client,
    schema_name: &str,
    username: &str,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Créer le schéma
    let create_schema = format!("CREATE SCHEMA {} AUTHORIZATION {}", schema_name, username);
    client.execute(&create_schema, &[]).await?;

    // Accorder tous les privilèges sur le schéma
    let grant_schema = format!("GRANT ALL PRIVILEGES ON SCHEMA {} TO {}", schema_name, username);
    client.execute(&grant_schema, &[]).await?;

    // Accorder les privilèges par défaut sur les tables
    let grant_tables = format!(
        "ALTER DEFAULT PRIVILEGES IN SCHEMA {} GRANT ALL ON TABLES TO {}",
        schema_name, username
    );
    client.execute(&grant_tables, &[]).await?;

    // Accorder les privilèges par défaut sur les séquences
    let grant_sequences = format!(
        "ALTER DEFAULT PRIVILEGES IN SCHEMA {} GRANT ALL ON SEQUENCES TO {}",
        schema_name, username
    );
    client.execute(&grant_sequences, &[]).await?;

    Ok(())
}
