import { Component, inject, OnInit, signal } from "@angular/core";
import { CommonModule, Location } from "@angular/common";
import { FormsModule } from "@angular/forms";

import { ButtonModule } from "primeng/button";
import { RadioButtonModule } from "primeng/radiobutton";
import { ToolbarModule } from "primeng/toolbar";

import { ConfigurationService, ModelReapproOption } from "app/shared/configuration.service";

@Component({
  selector: "app-semois-model-config",
  templateUrl: "./semois-model-config.component.html",
  styleUrls: ["./semois-model-config.component.scss"],
  imports: [CommonModule, FormsModule, ButtonModule, RadioButtonModule, ToolbarModule]
})
export class SemoisModelConfigComponent implements OnInit {
  readonly currentModel = signal<string>("CLASSIQUE");
  readonly selectedModel = signal<string>("CLASSIQUE");
  readonly availableModels = signal<ModelReapproOption[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly saveSuccess = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  private readonly configurationService = inject(ConfigurationService);
  private readonly location = inject(Location);

  ngOnInit(): void {
    this.loadConfiguration();
  }

  loadConfiguration(): void {
    this.isLoading.set(true);
    this.configurationService.getModelReappro().subscribe({
      next: res => {
        if (res.body) {
          this.currentModel.set(res.body.currentModel);
          this.selectedModel.set(res.body.currentModel);
          this.availableModels.set(res.body.availableModels);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set("Erreur lors du chargement de la configuration");
      }
    });
  }

  saveConfiguration(): void {
    this.isSaving.set(true);
    this.saveSuccess.set(false);
    this.errorMessage.set(null);
    this.configurationService.updateModelReappro(this.selectedModel()).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.saveSuccess.set(true);
        this.currentModel.set(this.selectedModel());
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: () => {
        this.isSaving.set(false);
        this.errorMessage.set("Erreur lors de la sauvegarde de la configuration");
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  hasChanges(): boolean {
    return this.currentModel() !== this.selectedModel();
  }
}
