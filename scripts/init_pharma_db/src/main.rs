#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use eframe::egui;
use tokio_postgres::{Client, NoTls, error::SqlState};
use std::sync::{Arc, Mutex};

const DEFAULT_PG_ADMIN_USER: &str = "postgres";
const DEFAULT_DB_NAME: &str = "pharma_smart";
const DEFAULT_USER: &str = "pharma_smart";

fn main() -> Result<(), eframe::Error> {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([600.0, 960.0])
            .with_resizable(false),
        ..Default::default()
    };

    eframe::run_native(
        "Initialisation Base de Données PharmaSmart",
        options,
        Box::new(|cc| {
            configure_custom_style(&cc.egui_ctx);
            Ok(Box::new(PharmaDbApp::default()))
        }),
    )
}

fn configure_custom_style(ctx: &egui::Context) {
    let mut style = (*ctx.style()).clone();

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

    style.spacing.item_spacing = egui::vec2(8.0, 12.0);
    style.spacing.button_padding = egui::vec2(16.0, 8.0);
    style.spacing.window_margin = egui::Margin::same(15.0);

    style.visuals.window_rounding = egui::Rounding::same(8.0);
    style.visuals.widgets.noninteractive.rounding = egui::Rounding::same(6.0);
    style.visuals.widgets.inactive.rounding = egui::Rounding::same(6.0);
    style.visuals.widgets.hovered.rounding = egui::Rounding::same(6.0);
    style.visuals.widgets.active.rounding = egui::Rounding::same(6.0);

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
    backup_user_pass: String,
    status_log: Arc<Mutex<Vec<LogEntry>>>,
    is_processing: Arc<Mutex<bool>>,
    success: Arc<Mutex<bool>>,
}

#[derive(Clone)]
struct LogEntry {
    kind: LogKind,
    message: String,
}

#[derive(Clone, PartialEq)]
enum LogKind {
    Info,
    Warning,
    Error,
    Success,
}

impl LogEntry {
    fn info(msg: impl Into<String>) -> Self { Self { kind: LogKind::Info, message: msg.into() } }
    fn warning(msg: impl Into<String>) -> Self { Self { kind: LogKind::Warning, message: msg.into() } }
    fn error(msg: impl Into<String>) -> Self { Self { kind: LogKind::Error, message: msg.into() } }
    fn success(msg: impl Into<String>) -> Self { Self { kind: LogKind::Success, message: msg.into() } }

    fn color(&self) -> egui::Color32 {
        match self.kind {
            LogKind::Info    => egui::Color32::from_rgb(60, 90, 150),
            LogKind::Warning => egui::Color32::from_rgb(160, 100, 20),
            LogKind::Error   => egui::Color32::from_rgb(180, 40, 40),
            LogKind::Success => egui::Color32::from_rgb(40, 120, 40),
        }
    }
}

impl PharmaDbApp {
    fn get_db_name(&self) -> String {
        if self.db_name.is_empty() { DEFAULT_DB_NAME.to_string() } else { self.db_name.clone() }
    }

    fn get_db_user(&self) -> String {
        if self.db_user.is_empty() { DEFAULT_USER.to_string() } else { self.db_user.clone() }
    }
}

impl eframe::App for PharmaDbApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        egui::CentralPanel::default().show(ctx, |ui| {
            ui.add_space(8.0);

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

            // Section : configuration base / utilisateur
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(240, 245, 255))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(200, 215, 240)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);

                    ui.label(egui::RichText::new("Configuration")
                        .size(16.0)
                        .strong()
                        .color(egui::Color32::from_rgb(60, 80, 140)));
                    ui.add_space(8.0);

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
                        ui.label(egui::RichText::new(hint_text).size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);

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
                        ui.label(egui::RichText::new(hint_text).size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);

                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0],
                            egui::Label::new(egui::RichText::new("Schéma:").size(13.0))
                        );
                        ui.label(egui::RichText::new(self.get_db_name())
                            .size(14.0).strong().color(egui::Color32::from_rgb(70, 130, 180)));
                    });

                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        ui.label(egui::RichText::new("→ Le schéma aura le même nom que la base")
                            .size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);

                    ui.horizontal(|ui| {
                        ui.set_min_height(28.0);
                        ui.add_sized([150.0, 28.0],
                            egui::Label::new(egui::RichText::new("User backup:").size(13.0))
                        );
                        let backup_user = format!("{}_backup", self.get_db_user());
                        ui.label(egui::RichText::new(backup_user)
                            .size(14.0).strong().color(egui::Color32::from_rgb(130, 80, 170)));
                    });

                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        ui.label(egui::RichText::new("→ Créé avec REPLICATION + pg_read_all_data")
                            .size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Section : identifiants admin PostgreSQL
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(252, 248, 245))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(230, 215, 200)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);

                    ui.label(egui::RichText::new("🔐 Identifiants Administrateur PostgreSQL")
                        .size(16.0).strong().color(egui::Color32::from_rgb(140, 80, 60)));
                    ui.add_space(10.0);

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

                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        let hint_text = if self.pg_admin_user.is_empty() {
                            format!("→ Valeur par défaut: {}", DEFAULT_PG_ADMIN_USER)
                        } else {
                            String::from(" ")
                        };
                        ui.label(egui::RichText::new(hint_text).size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);

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

                    ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Section : mot de passe utilisateur applicatif
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(245, 252, 245))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(200, 230, 200)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);

                    ui.label(egui::RichText::new("🔑 Mot de passe — Utilisateur applicatif")
                        .size(16.0).strong().color(egui::Color32::from_rgb(60, 120, 60)));
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
                        let user_hint = format!("→ Sera utilisé pour '{}'", self.get_db_user());
                        ui.label(egui::RichText::new(user_hint).size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Section : mot de passe utilisateur backup
            egui::Frame::none()
                .fill(egui::Color32::from_rgb(248, 245, 255))
                .rounding(8.0)
                .inner_margin(8.0)
                .stroke(egui::Stroke::new(1.5, egui::Color32::from_rgb(210, 200, 235)))
                .show(ui, |ui| {
                    ui.set_min_width(570.0);
                    ui.set_max_width(570.0);

                    ui.label(egui::RichText::new("🔒 Mot de passe — Utilisateur backup")
                        .size(16.0).strong().color(egui::Color32::from_rgb(100, 60, 160)));
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
                                    egui::TextEdit::singleline(&mut self.backup_user_pass)
                                        .password(true)
                                        .hint_text("••••••••")
                                        .frame(false)
                                );
                            });
                    });

                    ui.horizontal(|ui| {
                        ui.add_space(150.0);
                        let backup_hint = format!("→ Sera utilisé pour '{}_backup' (pg_dump / pg_basebackup)", self.get_db_user());
                        ui.label(egui::RichText::new(backup_hint).size(11.0).italics().color(egui::Color32::DARK_GRAY));
                    });

                    ui.add_space(1.5);
                });

            ui.add_space(12.0);

            // Bouton de création
            let is_processing = *self.is_processing.lock().unwrap();
            let can_submit = !self.pg_admin_pass.is_empty()
                && !self.new_user_pass.is_empty()
                && !self.backup_user_pass.is_empty()
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
                    let backup_pass = self.backup_user_pass.clone();
                    let db_name = self.get_db_name();
                    let db_user = self.get_db_user();
                    let status_log = Arc::clone(&self.status_log);
                    let is_processing = Arc::clone(&self.is_processing);
                    let success = Arc::clone(&self.success);
                    let ctx_clone = ctx.clone();

                    *is_processing.lock().unwrap() = true;
                    *status_log.lock().unwrap() = vec![LogEntry::info("Traitement en cours...")];

                    std::thread::spawn(move || {
                        let rt = tokio::runtime::Runtime::new().unwrap();
                        let result = rt.block_on(async {
                            execute_database_setup(
                                &admin_user, &admin_pass, &new_pass, &backup_pass,
                                &db_name, &db_user,
                            ).await
                        });

                        match result {
                            Ok(mut log) => {
                                log.push(LogEntry::success("✓ Configuration terminée avec succès!"));
                                *status_log.lock().unwrap() = log;
                                *success.lock().unwrap() = true;
                            }
                            Err((mut log, e)) => {
                                log.push(LogEntry::error(format!("✗ Erreur fatale: {}", e)));
                                *status_log.lock().unwrap() = log;
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

            // Journal de statut
            let log = self.status_log.lock().unwrap().clone();
            if !log.is_empty() {
                let success_state = *self.success.lock().unwrap();
                let (bg_color, border_color) = if is_processing {
                    (egui::Color32::from_rgb(240, 240, 245), egui::Color32::from_rgb(180, 180, 200))
                } else if success_state {
                    (egui::Color32::from_rgb(230, 250, 230), egui::Color32::from_rgb(150, 220, 150))
                } else {
                    (egui::Color32::from_rgb(255, 235, 235), egui::Color32::from_rgb(255, 150, 150))
                };

                egui::Frame::none()
                    .fill(bg_color)
                    .rounding(8.0)
                    .inner_margin(12.0)
                    .stroke(egui::Stroke::new(2.0, border_color))
                    .show(ui, |ui| {
                        ui.set_max_width(554.0);
                        if is_processing {
                            ui.horizontal(|ui| {
                                ui.spinner();
                                ui.add_space(6.0);
                                ui.label(egui::RichText::new("Traitement en cours...").size(13.0).color(egui::Color32::from_rgb(60, 80, 140)));
                            });
                            ui.add_space(4.0);
                        }
                        egui::ScrollArea::vertical()
                            .max_height(150.0)
                            .auto_shrink([false, true])
                            .show(ui, |ui| {
                                for entry in &log {
                                    ui.label(egui::RichText::new(&entry.message)
                                        .size(13.0)
                                        .color(entry.color()));
                                }
                            });
                    });
            }

            ui.add_space(10.0);
        });
    }
}

type SetupResult = Result<Vec<LogEntry>, (Vec<LogEntry>, Box<dyn std::error::Error + Send + Sync>)>;

fn is_already_exists(e: &tokio_postgres::Error) -> bool {
    matches!(
        e.code(),
        Some(c) if *c == SqlState::DUPLICATE_DATABASE
            || *c == SqlState::DUPLICATE_OBJECT
            || *c == SqlState::DUPLICATE_SCHEMA
    )
}

async fn execute_idempotent(
    client: &Client,
    sql: &str,
    skip_msg: &str,
    log: &mut Vec<LogEntry>,
) -> Result<(), tokio_postgres::Error> {
    match client.execute(sql, &[]).await {
        Ok(_) => Ok(()),
        Err(e) if is_already_exists(&e) => {
            log.push(LogEntry::warning(format!("⚠ {} (ignoré, déjà existant)", skip_msg)));
            Ok(())
        }
        Err(e) => Err(e),
    }
}

async fn execute_database_setup(
    admin_user: &str,
    admin_pass: &str,
    new_pass: &str,
    backup_pass: &str,
    db_name: &str,
    db_user: &str,
) -> SetupResult {
    let mut log = Vec::new();
    let new_schema = db_name;
    let backup_user = format!("{}_backup", db_user);

    // Connexion 1 : base postgres (superuser)
    let conn_str = format!(
        "host=localhost user={} password={} dbname=postgres",
        admin_user, admin_pass
    );
    log.push(LogEntry::info("Connexion à PostgreSQL (base postgres)..."));
    let (client, connection) = tokio_postgres::connect(&conn_str, NoTls).await
        .map_err(|e| (log.clone(), Box::new(e) as Box<dyn std::error::Error + Send + Sync>))?;

    tokio::spawn(async move {
        if let Err(e) = connection.await {
            eprintln!("Erreur connexion: {}", e);
        }
    });

    create_users_and_database(&client, db_name, db_user, new_pass, &backup_user, backup_pass, &mut log).await
        .map_err(|e| (log.clone(), e))?;

    // Connexion 2 : nouvelle base (superuser)
    let new_conn_str = format!(
        "host=localhost user={} password={} dbname={}",
        admin_user, admin_pass, db_name
    );
    log.push(LogEntry::info(format!("Connexion à la base '{}'...", db_name)));
    let (new_client, new_connection) = tokio_postgres::connect(&new_conn_str, NoTls).await
        .map_err(|e| (log.clone(), Box::new(e) as Box<dyn std::error::Error + Send + Sync>))?;

    tokio::spawn(async move {
        if let Err(e) = new_connection.await {
            eprintln!("Erreur connexion: {}", e);
        }
    });

    create_schema_and_grant_privileges(&new_client, new_schema, db_user, &backup_user, &mut log).await
        .map_err(|e| (log.clone(), e))?;

    Ok(log)
}

async fn create_users_and_database(
    client: &Client,
    db_name: &str,
    username: &str,
    password: &str,
    backup_user: &str,
    backup_pass: &str,
    log: &mut Vec<LogEntry>,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Base de données
    log.push(LogEntry::info(format!("Création de la base '{}'...", db_name)));
    execute_idempotent(client, &format!("CREATE DATABASE {}", db_name), &format!("base '{}'", db_name), log).await?;

    // Utilisateur applicatif
    log.push(LogEntry::info(format!("Création de l'utilisateur '{}'...", username)));
    execute_idempotent(
        client,
        &format!("CREATE USER {} WITH PASSWORD '{}'", username, password),
        &format!("utilisateur '{}'", username),
        log,
    ).await?;

    // Transfert de propriété (idempotent)
    client.execute(&format!("ALTER DATABASE {} OWNER TO {}", db_name, username), &[]).await?;
    log.push(LogEntry::info(format!("Propriété de '{}' transférée à '{}'.", db_name, username)));

    // Utilisateur backup
    log.push(LogEntry::info(format!("Création de l'utilisateur backup '{}'...", backup_user)));
    execute_idempotent(
        client,
        &format!("CREATE USER {} WITH LOGIN REPLICATION PASSWORD '{}'", backup_user, backup_pass),
        &format!("utilisateur backup '{}'", backup_user),
        log,
    ).await?;

    // GRANT CONNECT (idempotent en PostgreSQL)
    client.execute(&format!("GRANT CONNECT ON DATABASE {} TO {}", db_name, backup_user), &[]).await?;
    log.push(LogEntry::info(format!("GRANT CONNECT sur '{}' accordé à '{}'.", db_name, backup_user)));

    Ok(())
}

async fn create_schema_and_grant_privileges(
    client: &Client,
    schema_name: &str,
    username: &str,
    backup_user: &str,
    log: &mut Vec<LogEntry>,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Schéma — IF NOT EXISTS supporté par PostgreSQL
    log.push(LogEntry::info(format!("Création du schéma '{}'...", schema_name)));
    match client.execute(
        &format!("CREATE SCHEMA IF NOT EXISTS {} AUTHORIZATION {}", schema_name, username),
        &[],
    ).await {
        Ok(_) => log.push(LogEntry::info(format!("Schéma '{}' prêt.", schema_name))),
        Err(e) if is_already_exists(&e) => {
            log.push(LogEntry::warning(format!("⚠ Schéma '{}' déjà existant, ignoré.", schema_name)));
        }
        Err(e) => return Err(Box::new(e)),
    }

    // Droits complets utilisateur applicatif (idempotents)
    client.execute(&format!("GRANT ALL PRIVILEGES ON SCHEMA {} TO {}", schema_name, username), &[]).await?;
    client.execute(
        &format!("ALTER DEFAULT PRIVILEGES IN SCHEMA {} GRANT ALL ON TABLES TO {}", schema_name, username),
        &[],
    ).await?;
    client.execute(
        &format!("ALTER DEFAULT PRIVILEGES IN SCHEMA {} GRANT ALL ON SEQUENCES TO {}", schema_name, username),
        &[],
    ).await?;
    log.push(LogEntry::info(format!("Droits complets accordés à '{}' sur le schéma '{}'.", username, schema_name)));

    // Droits lecture seule utilisateur backup (idempotents)
    client.execute(&format!("GRANT pg_read_all_data TO {}", backup_user), &[]).await?;
    client.execute(&format!("GRANT USAGE ON SCHEMA {} TO {}", schema_name, backup_user), &[]).await?;
    log.push(LogEntry::info(format!("Droits lecture accordés à '{}' (pg_read_all_data + USAGE).", backup_user)));

    Ok(())
}
