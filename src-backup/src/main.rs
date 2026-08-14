use anyhow::Result;
use clap::{Parser, Subcommand};
use tracing_subscriber::EnvFilter;

mod base;
mod check;
mod config;
mod dump;
mod logger;
mod pg_discover;
mod purge;
mod sentinel;
#[cfg(test)]
mod testing;

#[derive(Parser)]
#[command(name = "pharmasmart-backup", version, about = "Outil de sauvegarde PharmaSmart")]
struct Cli {
    #[command(subcommand)]
    command: Cmd,
}

#[derive(Subcommand)]
enum Cmd {
    /// pg_dump logique — au plus max_daily_dumps (2) par jour
    Dump,
    /// pg_basebackup physique — hebdomadaire (auto-skip si < 6 j)
    Base,
    /// Rotation/purge des anciens fichiers (auto-skip si < 23 h)
    Purge,
    /// Vérifie la présence d'un dump récent (< max_dump_age_hours)
    Check,
}

fn main() -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")))
        .init();

    let cli = Cli::parse();
    let result = run(&cli.command);

    // Sous le Planificateur de tâches, stderr est invisible : toute erreur doit
    // laisser une trace sur disque, y compris quand config.json est introuvable.
    if let Err(err) = &result {
        logger::append_fallback(&format!("[ERREUR] {err:#}"));
    }
    result
}

fn run(cmd: &Cmd) -> Result<()> {
    let cfg = config::BackupConfig::load()?;
    match cmd {
        Cmd::Dump => dump::run(&cfg),
        Cmd::Base => base::run(&cfg),
        Cmd::Purge => purge::run(&cfg),
        Cmd::Check => check::run(&cfg),
    }
}
