import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ISemoisExclusion } from 'app/shared/model/semois';
import { SemoisService } from 'app/entities/semois/semois.service';

@Component({
  selector: 'app-semois-exclusion-panel',
  templateUrl: './semois-exclusion-panel.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, ButtonModule, TableModule, TooltipModule],
})
export class SemoisExclusionPanelComponent implements OnInit {
  private readonly activeModal = inject(NgbActiveModal);
  private readonly semoisService = inject(SemoisService);

  readonly exclusions = signal<ISemoisExclusion[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly liftingId = signal<number | null>(null);
  /** Nb de réintégrations effectuées dans cette session modal */
  reintegratedCount = 0;

  ngOnInit(): void {
    this.loadExclusions();
  }

  loadExclusions(): void {
    this.isLoading.set(true);
    this.semoisService.getExclusionsActives().subscribe({
      next: res => { this.exclusions.set(res.body ?? []); this.isLoading.set(false); },
      error: () => this.isLoading.set(false),
    });
  }

  leverExclusion(exclusion: ISemoisExclusion): void {
    this.liftingId.set(exclusion.produitId);
    this.semoisService.leverExclusion(exclusion.produitId).subscribe({
      next: () => {
        this.exclusions.update(list => list.filter(e => e.produitId !== exclusion.produitId));
        this.liftingId.set(null);
        this.reintegratedCount++;
      },
      error: () => this.liftingId.set(null),
    });
  }

  getJoursRestants(exclusion: ISemoisExclusion): number {
    const fin = new Date(exclusion.exclusionDateFin);
    const now = new Date();
    return Math.max(0, Math.ceil((fin.getTime() - now.getTime()) / 86_400_000));
  }

  close(): void {
    this.activeModal.close(this.reintegratedCount);
  }
}

