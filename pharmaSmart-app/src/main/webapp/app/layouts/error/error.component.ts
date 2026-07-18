import {Component, computed, inject, OnDestroy, OnInit, signal, ChangeDetectionStrategy} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from "@angular/common";
import {Subscription} from "rxjs";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: "jhi-error",
  templateUrl: "./error.component.html",
  styleUrl: "./error.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: []
})
export default class ErrorComponent implements OnInit, OnDestroy {
  errorMessage = signal<string | undefined>(undefined);
  errorKey = signal<string | undefined>(undefined);
  langChangeSubscription?: Subscription;
  protected readonly errorCode = computed(() => {
    const key = this.errorKey();
    if (key === "error.http.403") {
      return "403";
    }
    if (key === "error.http.404") {
      return "404";
    }
    return "500";
  });
  protected readonly errorIcon = computed(() => {
    const key = this.errorKey();
    if (key === "error.http.403") {
      return "pi pi-lock";
    }
    if (key === "error.http.404") {
      return "pi pi-search";
    }
    return "pi pi-exclamation-triangle";
  });
  protected readonly errorTitle = computed(() => {
    const key = this.errorKey();
    if (key === "error.http.403") {
      return "Accès refusé";
    }
    if (key === "error.http.404") {
      return "Page introuvable";
    }
    return "Une erreur est survenue";
  });
  private readonly translateService = inject(TranslateService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  ngOnInit(): void {
    this.route.data.subscribe(routeData => {
      if (routeData.errorMessage) {
        this.errorKey.set(routeData.errorMessage);
        this.getErrorMessageTranslation();
        this.langChangeSubscription = this.translateService.onLangChange.subscribe(() => this.getErrorMessageTranslation());
      }
    });
  }

  ngOnDestroy(): void {
    this.langChangeSubscription?.unsubscribe();
  }

  goBack(): void {
    this.location.back();
  }

  goHome(): void {
    this.router.navigate(["/"]);
  }

  private getErrorMessageTranslation(): void {
    this.errorMessage.set("");
    const key = this.errorKey();
    if (key) {
      this.translateService.get(key).subscribe(msg => this.errorMessage.set(msg));
    }
  }
}
