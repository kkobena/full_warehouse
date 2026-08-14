import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';

import {
  AppBadgeSeverity,
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  FileUploadComponent,
  ToolbarComponent
} from 'app/shared/ui';
import {NotificationService} from 'app/shared/services/notification.service';
import {AbilityService} from 'app/core/auth/ability.service';
import {LicenseService} from 'app/core/license/license.service';
import {
  ALL_FEATURES,
  LICENSE_AUDIT_LABEL,
  LICENSE_STATUS_LABEL,
  LICENSE_TYPE_LABEL,
  LicenseAuditEntry,
  LicenseStatus,
  PublisherContacts,
} from 'app/core/license/license.model';
import {ReactiveFormsModule} from "@angular/forms";
import {APPEND_TO} from "../../shared/constants/pagination.constants";

/**
 * Écran unique de gestion de la licence : première activation, renouvellement annuel et diagnostic.
 *
 * **Il doit rester atteignable licence expirée, invalide ou absente** — c'est la seule porte de
 * client, cette route n'est jamais soumise à une garde de licence.
 */
@Component({
  selector: 'app-license-admin',
  imports: [DatePipe, CardComponent, BadgeComponent, ButtonComponent, FileUploadComponent, ReactiveFormsModule, ToolbarComponent],
  templateUrl: './license-admin.component.html',
  styleUrl: './license-admin.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class LicenseAdminComponent implements OnInit {
  protected readonly licenseService = inject(LicenseService);
  protected readonly allFeatures = ALL_FEATURES;
  protected readonly typeLabels = LICENSE_TYPE_LABEL;
  protected readonly auditLabels = LICENSE_AUDIT_LABEL;
  protected readonly fingerprint = signal<string | null>(null);
  protected readonly auditEntries = signal<LicenseAuditEntry[]>([]);
  protected readonly pendingFile = signal<File | null>(null);
  protected readonly activating = signal(false);
  protected readonly activationError = signal<string | null>(null);
  protected readonly dragging = signal(false);
  protected readonly license = this.licenseService.license;
  protected readonly statusLabel = computed(() => {
    const status = this.license()?.status;
    return status ? LICENSE_STATUS_LABEL[status] : 'Inconnu';
  });
  protected readonly statusSeverity = computed<AppBadgeSeverity>(() => this.severityOf(this.license()?.status));
  protected readonly expiryLabel = computed(() => {
    const info = this.license();
    if (!info?.expiresAt) {
      return '—';
    }
    const days = info.daysRemaining;
    if (days < 0) {
      return `expirée depuis ${Math.abs(days)} jour(s)`;
    }
    return `${days} jour(s) restant(s)`;
  });
  protected readonly appendTo = APPEND_TO;
  protected readonly publisher = signal<PublisherContacts | null>(null);

  /**
   * Corps de la demande d'activation, à transmettre à l'éditeur.
   *
   * L'empreinte n'y figure que si elle a pu être lue : une licence liée à la seule raison sociale
   * s'émet sans elle, et c'est désormais le cas courant. L'éditeur la réclame explicitement quand
   * il décide de lier la licence au poste.
   */
  protected readonly activationRequest = computed(() => {
    const info = this.license();
    const lines = [
      `Officine : ${info?.magasinName ?? '(à compléter)'}`,
      `Empreinte du poste : ${this.fingerprint() ?? 'indisponible'}`,
      `Statut actuel : ${this.statusLabel()}`,
    ];
    if (info?.expiresAt) {
      lines.push(`Échéance : ${info.expiresAt}`);
    }
    return lines.join('\n');
  });

  protected readonly activationMailto = computed(() => {
    const to = this.publisher()?.emails?.[0] ?? '';
    const subject = `Demande d'activation — ${this.license()?.magasinName ?? 'officine'}`;
    return `mailto:${to}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(this.activationRequest())}`;
  });
  private readonly notification = inject(NotificationService);
  private readonly ability = inject(AbilityService);
  protected readonly canManage = this.ability.canSignal('execute', 'pr-gere-licence');

  ngOnInit(): void {
    this.licenseService.load().subscribe();
    this.licenseService.publisher().subscribe(contacts => this.publisher.set(contacts));
    if (this.canManage()) {
      this.loadFingerprint();
      this.loadAudit();
    }
  }

  /** Copie la demande complète (officine + empreinte), pour un envoi par le canal du client. */
  protected copyActivationRequest(): void {
    navigator.clipboard.writeText(this.activationRequest()).then(
      () => this.notification.success("Demande copiée. Transmettez-la à l'éditeur."),
      () => this.notification.error('La copie automatique a échoué : sélectionnez le texte et copiez-le manuellement.'),
    );
  }

  protected onFilesSelected(files: File[]): void {
    this.setPendingFile(files[0] ?? null);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDragLeave(): void {
    this.dragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.setPendingFile(event.dataTransfer?.files?.[0] ?? null);
  }

  protected activate(): void {
    const file = this.pendingFile();
    if (!file || this.activating()) {
      return;
    }
    this.activating.set(true);
    this.activationError.set(null);

    this.licenseService.activate(file).subscribe({
      next: info => {
        this.activating.set(false);
        this.pendingFile.set(null);
        this.notification.success(info.message, 'Licence activée');
        // La bannière disparaît et l'écriture est rétablie sans rechargement : le service a déjà
        // remplacé le signal avec la réponse d'activation, et le cache serveur a été invalidé.
        this.loadFingerprint();
        this.loadAudit();
      },
      error: (error: HttpErrorResponse) => {
        this.activating.set(false);
        // Le message du serveur explique la cause exacte (signature, officine, échéance) : on le
        // montre tel quel plutôt qu'un « échec de l'activation » qui n'aiderait personne.
        this.activationError.set(error.error?.message ?? error.error?.detail ?? "L'activation a échoué.");
      },
    });
  }

  protected copyFingerprint(): void {
    const value = this.fingerprint();
    if (!value) {
      return;
    }
    navigator.clipboard.writeText(value).then(
      () => this.notification.success('Empreinte copiée. Transmettez-la à votre revendeur.'),
      () => this.notification.error("La copie automatique a échoué : sélectionnez l'empreinte et copiez-la manuellement."),
    );
  }

  protected hasFeature(code: string): boolean {
    return this.licenseService.hasFeature(code);
  }

  protected auditSeverity(eventType: LicenseAuditEntry['eventType']): AppBadgeSeverity {
    return eventType === 'ACTIVATION' ? 'success' : eventType === 'BLOCKED_WRITE' ? 'warn' : 'danger';
  }

  private setPendingFile(file: File | null): void {
    this.activationError.set(null);
    this.pendingFile.set(file);
  }

  private loadFingerprint(): void {
    this.licenseService.fingerprint().subscribe({
      next: value => this.fingerprint.set(value),
      error: () => this.fingerprint.set(null),
    });
  }

  private loadAudit(): void {
    this.licenseService.audit().subscribe({
      next: entries => this.auditEntries.set(entries),
      error: () => this.auditEntries.set([]),
    });
  }

  private severityOf(status: LicenseStatus | undefined): AppBadgeSeverity {
    switch (status) {
      case 'VALID':
        return 'success';
      case 'EXPIRING_SOON':
        return 'info';
      case 'EXPIRING_CRITICAL':
      case 'GRACE':
        return 'warn';
      // Statut non chargé, ou contrôle temporairement indisponible : rien à alarmer.
      case undefined:
      case 'UNKNOWN':
        return 'secondary';
      default:
        return 'danger';
    }
  }
}
