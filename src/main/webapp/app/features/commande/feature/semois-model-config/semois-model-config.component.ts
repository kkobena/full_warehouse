import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { RadioButtonModule } from 'primeng/radiobutton';
import { ToolbarModule } from 'primeng/toolbar';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';

interface ModelReapproOption {
  value: string;
  label: string;
  description: string;
}

interface ModelReapproConfig {
  currentModel: string;
  availableModels: ModelReapproOption[];
}

@Component({
  selector: 'app-semois-model-config',
  templateUrl: './semois-model-config.component.html',
  styleUrls: ['./semois-model-config.component.scss'],
  imports: [CommonModule, FormsModule, ButtonModule, RadioButtonModule, ToolbarModule, WarehouseCommonModule],
})
export class SemoisModelConfigComponent implements OnInit {
  readonly currentModel = signal<string>('CLASSIQUE');
  readonly selectedModel = signal<string>('CLASSIQUE');
  readonly availableModels = signal<ModelReapproOption[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly saveSuccess = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  ngOnInit(): void {
    this.loadConfiguration();
  }

  loadConfiguration(): void {
    this.isLoading.set(true);
    const url = this.applicationConfigService.getEndpointFor('api/app/model-reappro');

    this.http.get<ModelReapproConfig>(url, { observe: 'response' }).subscribe({
      next: (res: HttpResponse<ModelReapproConfig>) => {
        if (res.body) {
          this.currentModel.set(res.body.currentModel);
          this.selectedModel.set(res.body.currentModel);
          this.availableModels.set(res.body.availableModels);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Erreur lors du chargement de la configuration');
      },
    });
  }

  saveConfiguration(): void {
    this.isSaving.set(true);
    this.saveSuccess.set(false);
    this.errorMessage.set(null);

    const url = this.applicationConfigService.getEndpointFor('api/app/model-reappro');

    this.http.put<void>(url, null, { params: { model: this.selectedModel() }, observe: 'response' }).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.saveSuccess.set(true);
        this.currentModel.set(this.selectedModel());
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: () => {
        this.isSaving.set(false);
        this.errorMessage.set('Erreur lors de la sauvegarde de la configuration');
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/semois/suggestions']);
  }

  hasChanges(): boolean {
    return this.currentModel() !== this.selectedModel();
  }
}

