import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InputNumberComponent } from './input-number.component';

describe('InputNumberComponent', () => {
  let fixture: ComponentFixture<InputNumberComponent>;
  let component: InputNumberComponent;

  const input = (): HTMLInputElement => {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('input');
  };

  /** Simule une saisie utilisateur complète : focus, frappe, puis blur. */
  const type = (text: string): void => {
    const el = input();
    el.dispatchEvent(new Event('focus'));
    el.value = text;
    el.dispatchEvent(new Event('input'));
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [InputNumberComponent] }).compileComponents();
    fixture = TestBed.createComponent(InputNumberComponent);
    component = fixture.componentInstance;
  });

  describe('affichage', () => {
    it('formate à la française au repos', () => {
      component.writeValue(1234567);
      expect(input().value).toBe(new Intl.NumberFormat('fr-FR').format(1234567));
    });

    it('affiche une chaîne vide pour null', () => {
      component.writeValue(null);
      expect(input().value).toBe('');
    });

    it('applique prefix et suffix au repos', () => {
      fixture.componentRef.setInput('suffix', ' F CFA');
      component.writeValue(1500);
      expect(input().value).toMatch(/ F CFA$/);
    });

    it('respecte maxFractionDigits', () => {
      fixture.componentRef.setInput('maxFractionDigits', 2);
      component.writeValue(12.345);
      expect(input().value).toContain('12,35');
    });

    it('bascule sur la valeur brute pendant la saisie', () => {
      component.writeValue(1234);
      input().dispatchEvent(new Event('focus'));
      // Sans ça le formatage ferait sauter le curseur à chaque frappe.
      expect(input().value).toBe('1234');
    });

    it('présente la virgule décimale à la prise de focus', () => {
      component.writeValue(12.5);
      input().dispatchEvent(new Event('focus'));
      expect(input().value).toBe('12,5');
    });
  });

  describe('parsing', () => {
    it.each([
      ['1234', 1234],
      ['12,5', 12.5],
      ['12.5', 12.5],
      ['1 234,5', 1234.5],
      ['-42', -42],
    ])('interprète "%s" comme %s', (raw, expected) => {
      const onChange = jest.fn();
      component.registerOnChange(onChange);
      type(raw);
      expect(onChange).toHaveBeenLastCalledWith(expected);
    });

    it.each([['', null], ['-', null], ['abc', null]])('interprète "%s" comme null', (raw, expected) => {
      const onChange = jest.fn();
      component.registerOnChange(onChange);
      type(raw);
      expect(onChange).toHaveBeenLastCalledWith(expected);
    });
  });

  describe('bornes', () => {
    it('ramène au minimum au blur', () => {
      const onChange = jest.fn();
      component.registerOnChange(onChange);
      fixture.componentRef.setInput('min', 0);
      type('-5');
      input().dispatchEvent(new Event('blur'));
      expect(onChange).toHaveBeenLastCalledWith(0);
    });

    it('ramène au maximum au blur', () => {
      const onChange = jest.fn();
      component.registerOnChange(onChange);
      fixture.componentRef.setInput('max', 100);
      type('150');
      input().dispatchEvent(new Event('blur'));
      expect(onChange).toHaveBeenLastCalledWith(100);
    });

    it('laisse passer une valeur dans les bornes', () => {
      const onChange = jest.fn();
      component.registerOnChange(onChange);
      fixture.componentRef.setInput('min', 0);
      fixture.componentRef.setInput('max', 100);
      type('50');
      input().dispatchEvent(new Event('blur'));
      expect(onChange).toHaveBeenLastCalledWith(50);
    });
  });

  describe('ControlValueAccessor', () => {
    it('setDisabledState désactive le champ', () => {
      component.setDisabledState(true);
      expect(input().disabled).toBe(true);
    });

    it('notifie onTouched au blur', () => {
      const onTouched = jest.fn();
      component.registerOnTouched(onTouched);
      input().dispatchEvent(new Event('blur'));
      expect(onTouched).toHaveBeenCalled();
    });

    it('invalid ajoute .is-invalid', () => {
      fixture.componentRef.setInput('invalid', true);
      expect([...input().classList]).toContain('is-invalid');
    });
  });
});
