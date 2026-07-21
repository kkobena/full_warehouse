import { HttpErrorResponse } from "@angular/common/http";
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
  ChangeDetectionStrategy
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from "@angular/forms";
import { concat, EMPTY } from "rxjs";
import { switchMap, toArray } from "rxjs/operators";
import { PosteService } from "../poste.service";
import { IPoste, Poste } from "../../../../../shared/model/poste.model";
import { NgbActiveModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { ErrorService } from "../../../../../shared/error.service";
import { TauriDeviceDetectionService } from "../../../../../shared/services/tauri-device-detection.service";
import { PosteDeviceService } from "../poste-device.service";
import {
  DeviceType,
  IPosteDevice,
  ISerialPortDetail,
  ISystemInfo
} from "../../../../../shared/model/poste-device.model";
import { NotificationService } from "../../../../../shared/services/notification.service";
import { CommonModule } from "@angular/common";
import { BadgeComponent, ButtonComponent, CardComponent, SelectComponent } from "../../../../../shared/ui";

@Component({
  selector: "app-form-poste",
  templateUrl: "./form-poste.component.html",
  styleUrl: "./form-poste.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    SelectComponent,
    BadgeComponent,
    NgbTooltip
  ]
})
export class FormPosteComponent implements OnInit, AfterViewInit, OnDestroy {
  title = "";
  entity: IPoste;
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required, Validators.maxLength(100)]],
    posteNumber: [null, [Validators.maxLength(20)]],
    address: [
      null,
      [Validators.required, Validators.pattern(/^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/)]
    ]
  });

  // ── Détection automatique des ports série via Tauri ──
  protected readonly detectedPorts = signal<ISerialPortDetail[]>([]);
  protected readonly portsLoading = signal(false);
  protected readonly isTauri = signal(false);
  protected readonly scannerTestResult = signal<"idle" | "waiting" | "ok" | "error">("idle");
  protected readonly scannerErrorMessage = signal<string>("");
  protected readonly displayTestResult = signal<"idle" | "waiting" | "ok" | "error">("idle");
  protected readonly portOptions = signal<{ label: string; value: string }[]>([]);
  protected readonly devices = signal<IPosteDevice[]>([]);
  protected readonly pendingDevices = signal<IPosteDevice[]>([]);

  private readonly entityService = inject(PosteService);
  private readonly notificationService = inject(NotificationService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private scanTestCleanup: (() => void) | null = null;
  private readonly tauriDeviceService = inject(TauriDeviceDetectionService);
  private readonly posteDeviceService = inject(PosteDeviceService);
  private nameInput = viewChild.required<ElementRef>("nameInput");

  ngOnInit(): void {
    this.isTauri.set(this.tauriDeviceService.isTauriAvailable());
    if (this.entity) {
      this.updateForm(this.entity);
      if (this.entity.id) {
        this.loadDevices(this.entity.id);
      }
    }

    if (this.isTauri()) {
      this.refreshPorts();
      // Pré-remplir le nom et l'IP du poste si le formulaire est vide (création)
      if (!this.entity?.id) {
        this.autoDetectSystemInfo();
      }
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.nameInput().nativeElement.focus();
    }, 100);
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id) {
      // Mode édition — pas de devices à envoyer ici
      this.entityService.create(entity).subscribe({
        next: () => this.onSaveSuccess(),
        error: err => this.onSaveError(err)
      });
    } else {
      // Mode création — on crée le poste puis on envoie les devices en attente
      this.entityService.create(entity).pipe(
        switchMap(response => {
          const createdPoste = response.body;
          if (!createdPoste?.id || this.pendingDevices().length === 0) {
            return EMPTY;
          }
          const deviceRequests = this.pendingDevices().map(d =>
            this.posteDeviceService.create(createdPoste.id!, { ...d, posteId: createdPoste.id })
          );
          return concat(...deviceRequests).pipe(toArray());
        })
      ).subscribe({
        next: () => this.onSaveSuccess(),
        error: err => this.onSaveError(err)
      });
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  /** Lister les ports série via Tauri */
  protected async refreshPorts(): Promise<void> {
    if (!this.isTauri()) return;
    this.portsLoading.set(true);
    try {
      const ports = await this.tauriDeviceService.listSerialPorts();
      this.detectedPorts.set(ports);
      this.portOptions.set(
        ports.map(p => ({
          label: this.buildPortLabel(p),
          value: p.portName
        }))
      );
    } catch {
      // Tauri command failed
    } finally {
      this.portsLoading.set(false);
    }
  }

  /** Pré-remplir le nom et l'IP du poste via détection Tauri */
  private async autoDetectSystemInfo(): Promise<void> {
    try {
      const info: ISystemInfo | null = await this.tauriDeviceService.getSystemInfo();
      if (info) {
        const currentName = this.editForm.get("name")?.value;
        const currentAddress = this.editForm.get("address")?.value;
        if (!currentName) {
          this.editForm.patchValue({ name: info.hostname });
        }
        if (!currentAddress) {
          this.editForm.patchValue({ address: info.localIp });
        }
      }
    } catch {
      // Détection non disponible — l'utilisateur saisira manuellement
    }
  }

  /** Construit un libellé descriptif pour un port série détecté */
  private buildPortLabel(port: ISerialPortDetail): string {

    const parts: string[] = [port.portName];
    // Afficher le chipset ou le fabricant entre crochets
    if (port.chipset) {
      parts.push(`[${port.chipset}]`);
    } else if (port.manufacturer) {
      parts.push(`[${port.manufacturer}]`);
    }
    // Afficher le nom du produit (nettoyé par Rust, sans le "(COMx)" redondant)
    if (port.product) {
      parts.push(`— ${port.product}`);
    }
    if (port.suggestedRole) {
      parts.push(`💡 ${this.roleLabel(port.suggestedRole)}`);
    }
    return parts.join(" ");
  }

  /** Tester le scanner sur un port donné */
  protected async testScanner(portName: string, baudRate: number): Promise<void> {
    if (!portName || !this.isTauri()) return;
    this.scannerTestResult.set("waiting");
    this.scannerErrorMessage.set("");
    // Annuler un test en cours si relancé
    this.scanTestCleanup?.();
    this.scanTestCleanup = null;
    try {
      const { listen } = await import("@tauri-apps/api/event");
      // Enregistrer les listeners AVANT de démarrer le scanner pour éviter toute race condition
      let unlistenScan: (() => void) | null = null;
      let unlistenError: (() => void) | null = null;
      let timer: ReturnType<typeof setTimeout> | null = null;
      let cleaned = false;
      const cleanup = async () => {
        if (cleaned) return;
        cleaned = true;
        if (timer !== null) clearTimeout(timer);
        unlistenScan?.();
        unlistenError?.();
        this.scanTestCleanup = null;
        await this.tauriDeviceService.stopScannerListener();
      };
      this.scanTestCleanup = cleanup;
      [unlistenScan, unlistenError] = await Promise.all([
        listen<string>("scan-test", async () => {
          await cleanup();
          this.scannerTestResult.set("ok");
        }),
        listen<string>("scan-error", async event => {
          await cleanup();
          this.scannerErrorMessage.set(event.payload ?? "Erreur port série");
          this.scannerTestResult.set("error");
        })
      ]);
      timer = setTimeout(async () => {
        await cleanup();
        this.scannerTestResult.set("error");
        this.scannerErrorMessage.set("Aucun scan reçu dans les 5 secondes");
      }, 5000);
      await this.tauriDeviceService.startScannerListener(portName, baudRate, "scan-test");
    } catch (e: any) {
      this.scannerErrorMessage.set(e?.message ?? "Erreur de démarrage");
      this.scannerTestResult.set("error");
    }
  }

  ngOnDestroy(): void {
    this.scanTestCleanup?.();
  }

  /** Tester l'afficheur client */
  protected async testDisplay(portName: string): Promise<void> {
    if (!portName || !this.isTauri()) return;
    this.displayTestResult.set("waiting");
    try {
      await this.tauriDeviceService.sendToDisplay(portName, "PHARMA-SMART", 9600);
      this.displayTestResult.set("ok");
    } catch {
      this.displayTestResult.set("error");
    }
  }

  protected readonly deviceTypeOptions = [
    { label: "Scanner", value: "SCANNER" },
    { label: "Afficheur", value: "DISPLAY" },
    { label: "Imprimante", value: "PRINTER" }
  ];

  protected roleLabel(role: string): string {
    switch (role) {
      case "scanner":
        return "Scanner";
      case "display":
        return "Afficheur";
      case "printer":
        return "Imprimante";
      default:
        return role;
    }
  }

  protected activateDevice(device: IPosteDevice): void {
    if (device.id && this.entity?.id) {
      this.posteDeviceService.activate(this.entity.id, device.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.loadDevices(this.entity.id!));
    }
  }

  protected deleteDevice(device: IPosteDevice): void {
    if (device.id && this.entity?.id) {
      // Mode édition — suppression backend
      this.posteDeviceService.delete(this.entity.id, device.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.loadDevices(this.entity.id!));
    } else {
      // Mode création — suppression locale
      this.pendingDevices.update(list => list.filter(d => d.portName !== device.portName || d.deviceType !== device.deviceType));
      this.devices.update(list => list.filter(d => d.portName !== device.portName || d.deviceType !== device.deviceType));
    }
  }

  protected addDevice(port: ISerialPortDetail, deviceType: DeviceType): void {
    if (!deviceType) return;
    const newDevice: IPosteDevice = {
      posteId: this.entity?.id,
      deviceType,
      portName: port.portName,
      label: port.chipset ?? port.product ?? port.manufacturer ?? undefined,
      baudRate: 9600,
      vid: port.vid,
      pid: port.pid,
      manufacturer: port.manufacturer ?? undefined,
      productName: port.product ?? undefined,
      serialNumber: port.serialNumber ?? undefined,
      active: false
    };

    if (this.entity?.id) {
      // Mode édition — envoi immédiat au backend
      this.posteDeviceService.create(this.entity.id, newDevice)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.loadDevices(this.entity.id!));
    } else {
      // Mode création — accumulation locale (envoyé lors du save)
      this.pendingDevices.update(list => [...list, newDevice]);
      this.devices.update(list => [...list, newDevice]);
    }
  }

  private loadDevices(posteId: number): void {
    this.posteDeviceService.fetchAll(posteId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      this.devices.set(res.body ?? []);
    });
  }

  private updateForm(entity: IPoste): void {
    this.editForm.patchValue({
      id: entity.id,
      name: entity.name,
      posteNumber: entity.posteNumber,
      address: entity.address
    });
  }


  private onSaveSuccess(): void {
    this.activeModal.close();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IPoste {
    return {
      ...new Poste(),
      id: this.editForm.get(["id"]).value,
      name: this.editForm.get(["name"]).value,
      posteNumber: this.editForm.get(["posteNumber"]).value,
      address: this.editForm.get(["address"]).value
    };
  }
}
