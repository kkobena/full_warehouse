import { Component, effect, inject, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModal, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IRayon, TYPE_ZONE_SEVERITY } from '../../models/rayon.model';
import { RayonProduitsTabComponent } from '../rayon-produits-tab/rayon-produits-tab.component';
import { RayonFormComponent } from '../rayon-form/rayon-form.component';
import { CloneRayonProduitsFormComponent } from '../clone-rayon-produits-form/clone-rayon-produits-form.component';
import { InventoryCreateModalComponent } from '../../../../features/inventory/ui/inventory-create-modal/inventory-create-modal.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { IResponseDto } from '../../../../shared/util/response-dto';

@Component({
  selector: 'app-rayon-detail-panel',
  templateUrl: './rayon-detail-panel.component.html',
  styleUrl: './rayon-detail-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [NgbNavModule, ButtonModule, TagModule, TooltipModule, RayonProduitsTabComponent],
})
export class RayonDetailPanelComponent {
  readonly rayon = input.required<IRayon>();

  readonly closed = output<void>();
  readonly edited = output<IRayon>();
  readonly deleteRequested = output<IRayon>();

  protected activeTab = signal('produits');

  private readonly modalService = inject(NgbModal);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);
  private currentRayonId: number | null = null;

  constructor() {
    effect(() => {
      const r = this.rayon();
      if (r?.id !== this.currentRayonId) {
        this.currentRayonId = r?.id ?? null;
        this.activeTab.set('produits');
      }
    });
  }

  protected onEdit(): void {
    const ref = this.modalService.open(RayonFormComponent, {
      size: 'lg',
      centered: true,
      backdrop: 'static',
    });
    const inst = ref.componentInstance as RayonFormComponent;
    inst.entity = { ...this.rayon() };
    inst.header = 'Modifier ' + this.rayon().libelle;
    ref.closed.subscribe((saved: IRayon) => {
      if (saved) this.edited.emit(saved);
    });
  }

  protected onDelete(): void {
    this.deleteRequested.emit(this.rayon());
  }

  protected onLancerInventaire(): void {
    const rayon = this.rayon();
    const ref = this.modalService.open(InventoryCreateModalComponent, {
      size: 'lg',
      centered: true,
      backdrop: 'static',
    });
    const inst = ref.componentInstance as InventoryCreateModalComponent;
    inst.prefill = { inventoryCategory: 'RAYON', storageId: rayon.storageId, rayonId: rayon.id };
    ref.closed.subscribe((inventory: { id?: number }) => {
      if (inventory?.id) {
        this.router.navigate(['/inventaire', inventory.id, 'edit']);
      }
    });
  }

  protected onClonerVers(): void {
    const ref = this.modalService.open(CloneRayonProduitsFormComponent, {
      size: 'md',
      centered: true,
      backdrop: 'static',
    });
    (ref.componentInstance as CloneRayonProduitsFormComponent).rayon = this.rayon();
    ref.closed.subscribe((result: IResponseDto) => {
      if (result) this.notificationService.success(result.message ?? 'Clonage effectué');
    });
  }

  protected onTabChange(tab: string | number): void {
    this.activeTab.set(String(tab));
  }

  protected typeZoneSev(typeZone?: string): string {
    if (!typeZone) return 'secondary';
    return TYPE_ZONE_SEVERITY[typeZone as keyof typeof TYPE_ZONE_SEVERITY] ?? 'secondary';
  }
}
