import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';

import { CheckboxComponent } from '../checkbox/checkbox.component';
import { InputComponent } from '../input/input.component';
import { PasswordComponent } from '../password/password.component';
import { RadioComponent } from '../radio/radio.component';
import { SwitchComponent } from '../switch/switch.component';

/**
 * Contrat `ControlValueAccessor` commun — vérifié à l'identique sur chaque composant
 * de formulaire, puisque tous héritent de `ControlValueAccessorBase`.
 */
describe.each([
  ['CheckboxComponent', CheckboxComponent as Type<unknown>, true],
  ['SwitchComponent', SwitchComponent as Type<unknown>, true],
  ['InputComponent', InputComponent as Type<unknown>, 'texte'],
  ['PasswordComponent', PasswordComponent as Type<unknown>, 'secret'],
])('%s — contrat ControlValueAccessor', (_name, componentType, sampleValue) => {
  let fixture: ComponentFixture<any>;
  let component: any;

  const input = (): HTMLInputElement => {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('input');
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [componentType] }).compileComponents();
    fixture = TestBed.createComponent(componentType);
    component = fixture.componentInstance;
  });

  it('writeValue reporte la valeur sur le champ', () => {
    component.writeValue(sampleValue);
    const el = input();
    expect(typeof sampleValue === 'boolean' ? el.checked : el.value).toBe(sampleValue);
  });

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

  it('notifie onChange à la saisie', () => {
    const onChange = jest.fn();
    component.registerOnChange(onChange);
    const el = input();
    if (typeof sampleValue === 'boolean') {
      el.checked = true;
      el.dispatchEvent(new Event('change'));
    } else {
      el.value = sampleValue;
      el.dispatchEvent(new Event('input'));
    }
    expect(onChange).toHaveBeenCalledWith(sampleValue);
  });
});

describe('CheckboxComponent', () => {
  let fixture: ComponentFixture<CheckboxComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CheckboxComponent] }).compileComponents();
    fixture = TestBed.createComponent(CheckboxComponent);
  });

  it('lie le libellé au champ pour que le clic dessus coche', () => {
    fixture.componentRef.setInput('label', 'Accepter');
    fixture.detectChanges();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    const label: HTMLLabelElement = fixture.nativeElement.querySelector('label');
    expect(label.htmlFor).toBe(input.id);
    expect(input.id).not.toBe('');
  });

  it("n'affiche pas de label quand aucun libellé n'est fourni", () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('label')).toBeNull();
  });
});

describe('SwitchComponent', () => {
  it('expose role="switch" pour les lecteurs d\'écran', async () => {
    await TestBed.configureTestingModule({ imports: [SwitchComponent] }).compileComponents();
    const fixture = TestBed.createComponent(SwitchComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input').getAttribute('role')).toBe('switch');
    expect([...fixture.nativeElement.querySelector('.form-check').classList]).toContain('form-switch');
  });
});

describe('RadioComponent', () => {
  let fixture: ComponentFixture<RadioComponent>;
  let component: RadioComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RadioComponent] }).compileComponents();
    fixture = TestBed.createComponent(RadioComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('name', 'mode');
    fixture.componentRef.setInput('value', 'especes');
  });

  const input = (): HTMLInputElement => {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('input');
  };

  it('est coché quand le modèle vaut sa propre valeur', () => {
    component.writeValue('especes');
    expect(input().checked).toBe(true);
  });

  it("n'est pas coché quand le modèle vaut autre chose", () => {
    component.writeValue('carte');
    expect(input().checked).toBe(false);
  });

  it('émet sa propre valeur à la sélection', () => {
    const onChange = jest.fn();
    component.registerOnChange(onChange);
    input().dispatchEvent(new Event('change'));
    expect(onChange).toHaveBeenCalledWith('especes');
  });

  it('reporte le name, qui assure l\'exclusivité native du groupe', () => {
    expect(input().name).toBe('mode');
  });
});

describe('PasswordComponent', () => {
  let fixture: ComponentFixture<PasswordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PasswordComponent] }).compileComponents();
    fixture = TestBed.createComponent(PasswordComponent);
  });

  const input = (): HTMLInputElement => {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('input');
  };

  it('masque la saisie par défaut', () => {
    expect(input().type).toBe('password');
  });

  it('révèle puis remasque la saisie', () => {
    fixture.detectChanges();
    const toggle: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    toggle.click();
    expect(input().type).toBe('text');
    toggle.click();
    expect(input().type).toBe('password');
  });

  it('expose un aria-label et un aria-pressed sur le bouton', () => {
    fixture.detectChanges();
    const toggle: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(toggle.getAttribute('aria-pressed')).toBe('false');
    expect(toggle.getAttribute('aria-label')).toBe('Afficher le mot de passe');
    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-label')).toBe('Masquer le mot de passe');
  });

  it('masque le bouton quand toggleMask est désactivé', () => {
    fixture.componentRef.setInput('toggleMask', false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });
});

describe('InputComponent', () => {
  let fixture: ComponentFixture<InputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [InputComponent] }).compileComponents();
    fixture = TestBed.createComponent(InputComponent);
  });

  it.each([
    ['small', 'form-control-sm'],
    ['large', 'form-control-lg'],
  ] as const)('size "%s" ajoute .%s', (size, expected) => {
    fixture.componentRef.setInput('size', size);
    fixture.detectChanges();
    expect([...fixture.nativeElement.querySelector('input').classList]).toContain(expected);
  });

  it('invalid ajoute .is-invalid', () => {
    fixture.componentRef.setInput('invalid', true);
    fixture.detectChanges();
    expect([...fixture.nativeElement.querySelector('input').classList]).toContain('is-invalid');
  });
});
