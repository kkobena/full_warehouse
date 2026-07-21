import { TestBed } from '@angular/core/testing';

import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
  });

  it('démarre sans notification', () => {
    expect(service.messages()).toEqual([]);
  });

  describe('API publique — inchangée depuis la version PrimeNG', () => {
    it.each([
      ['success', 'success', 'Succès', 3000],
      ['info', 'info', 'Information', 3000],
      ['warning', 'warn', 'Avertissement', 4000],
      ['error', 'error', 'Erreur', 5000],
    ] as const)('%s() empile une notification "%s"', (method, severity, defaultTitle, life) => {
      service[method]('Message métier');
      expect(service.messages()).toEqual([
        expect.objectContaining({ severity, summary: defaultTitle, detail: 'Message métier', life }),
      ]);
    });

    it('accepte un titre explicite', () => {
      service.success('Enregistré', 'Produit');
      expect(service.messages()[0].summary).toBe('Produit');
    });

    it('show() permet severity et durée sur mesure', () => {
      service.show('warn', 'Stock bas', 'Alerte', 8000);
      expect(service.messages()[0]).toEqual(
        expect.objectContaining({ severity: 'warn', summary: 'Alerte', detail: 'Stock bas', life: 8000 }),
      );
    });

    it('clear() vide la pile', () => {
      service.success('A');
      service.error('B');
      service.clear();
      expect(service.messages()).toEqual([]);
    });
  });

  describe('empilement', () => {
    it('conserve l\'ordre d\'arrivée', () => {
      service.success('Premier');
      service.error('Second');
      expect(service.messages().map(m => m.detail)).toEqual(['Premier', 'Second']);
    });

    it('attribue des identifiants uniques', () => {
      service.success('A');
      service.success('B');
      const [first, second] = service.messages();
      expect(first.id).not.toBe(second.id);
    });

    it('dismiss() ne retire que la notification visée', () => {
      service.success('A');
      service.success('B');
      service.dismiss(service.messages()[0].id);
      expect(service.messages().map(m => m.detail)).toEqual(['B']);
    });

    it('dismiss() sur un id inconnu ne casse rien', () => {
      service.success('A');
      service.dismiss(999);
      expect(service.messages()).toHaveLength(1);
    });
  });
});
