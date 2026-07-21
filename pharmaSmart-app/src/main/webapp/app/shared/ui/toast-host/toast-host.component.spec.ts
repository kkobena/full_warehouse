import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap';

import { NotificationService } from 'app/shared/services/notification.service';
import { ToastHostComponent } from './toast-host.component';

describe('ToastHostComponent', () => {
  let fixture: ComponentFixture<ToastHostComponent>;
  let notifications: NotificationService;

  const render = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement;
  };

  const toasts = (): HTMLElement[] => [...render().querySelectorAll('ngb-toast')] as HTMLElement[];

  const firstToast = (): DebugElement => {
    fixture.detectChanges();
    return fixture.debugElement.query(By.directive(NgbToast));
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ToastHostComponent] }).compileComponents();
    fixture = TestBed.createComponent(ToastHostComponent);
    notifications = TestBed.inject(NotificationService);
  });

  it('n\'affiche rien tant qu\'aucune notification n\'est émise', () => {
    expect(toasts()).toHaveLength(0);
  });

  it('rend une notification par message', () => {
    notifications.success('Produit enregistré');
    notifications.error('Échec du paiement');
    expect(toasts()).toHaveLength(2);
  });

  it('affiche le titre et le message', () => {
    notifications.success('Produit enregistré', 'Stock');
    const toast = toasts()[0];
    expect(toast.textContent).toContain('Stock');
    expect(toast.textContent).toContain('Produit enregistré');
  });

  it.each([
    ['success', 'text-bg-success', 'pi-check-circle'],
    ['info', 'text-bg-info', 'pi-info-circle'],
    ['warn', 'text-bg-warning', 'pi-exclamation-triangle'],
    ['error', 'text-bg-danger', 'pi-times-circle'],
  ] as const)('habille la severity "%s"', (severity, expectedClass, expectedIcon) => {
    notifications.show(severity, 'Message');
    expect([...toasts()[0].classList]).toContain(expectedClass);
    expect(toasts()[0].querySelector(`i.${expectedIcon}`)).not.toBeNull();
  });

  it('transmet la durée de vie à ngb-toast', () => {
    notifications.error('Erreur'); // 5000 ms
    // `delay` pilote l'auto-fermeture : c'est NgbToast qui la gère, pas le service.
    expect((firstToast().componentInstance as NgbToast).delay).toBe(5000);
  });

  it('retire la notification quand ngb-toast signale sa fermeture', () => {
    notifications.success('Message');
    firstToast().triggerEventHandler('hidden', undefined);
    expect(notifications.messages()).toHaveLength(0);
    expect(toasts()).toHaveLength(0);
  });

  it('reste au-dessus des modales', () => {
    // Une confirmation ngb monte à z-index 1055 ; les toasts doivent rester lisibles.
    expect(render().querySelector('.toast-container')).not.toBeNull();
  });
});
