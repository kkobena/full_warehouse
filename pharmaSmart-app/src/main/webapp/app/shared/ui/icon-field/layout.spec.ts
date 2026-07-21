import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { FloatLabelComponent } from '../float-label/float-label.component';
import { FormFieldComponent } from '../form-field/form-field.component';
import { IconFieldComponent } from './icon-field.component';
import { SkeletonComponent } from '../skeleton/skeleton.component';

describe('IconFieldComponent', () => {
  @Component({
    imports: [IconFieldComponent],
    template: `
      <app-icon-field [icon]="'pi pi-search'" [iconPos]="pos">
        <input class="form-control" />
      </app-icon-field>
    `,
  })
  class HostComponent {
    pos: 'left' | 'right' = 'left';
  }

  const render = (pos: 'left' | 'right' = 'left'): HTMLElement => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.pos = pos;
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('app-icon-field');
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
  });

  it('rend un input-group Bootstrap', () => {
    expect([...render().classList]).toContain('input-group');
  });

  it("place l'icône avant le champ par défaut", () => {
    const children = [...render().children].map(el => el.tagName.toLowerCase());
    expect(children[0]).toBe('span');
    expect(children[children.length - 1]).toBe('input');
  });

  it("place l'icône après le champ quand iconPos vaut right", () => {
    const children = [...render('right').children].map(el => el.tagName.toLowerCase());
    expect(children[0]).toBe('input');
    expect(children[children.length - 1]).toBe('span');
  });
});

describe('FloatLabelComponent', () => {
  it('rend .form-floating et lie le label au champ projeté', async () => {
    await TestBed.configureTestingModule({ imports: [FloatLabelComponent] }).compileComponents();
    const fixture = TestBed.createComponent(FloatLabelComponent);
    fixture.componentRef.setInput('label', 'Nom');
    fixture.componentRef.setInput('inputId', 'produit-nom');
    fixture.detectChanges();

    const host: HTMLElement = fixture.nativeElement;
    expect([...host.classList]).toContain('form-floating');
    expect(host.querySelector('label')!.htmlFor).toBe('produit-nom');
    expect(host.querySelector('label')!.textContent?.trim()).toBe('Nom');
  });
});

describe('SkeletonComponent', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<SkeletonComponent>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SkeletonComponent] }).compileComponents();
    fixture = TestBed.createComponent(SkeletonComponent);
  });

  const placeholder = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('.placeholder');
  };

  it('utilise les classes placeholder de Bootstrap', () => {
    expect(placeholder()).not.toBeNull();
    expect([...fixture.nativeElement.classList]).toContain('placeholder-glow');
  });

  it('est masqué aux lecteurs d\'écran', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.getAttribute('aria-hidden')).toBe('true');
  });

  it('applique largeur et hauteur en forme rectangle', () => {
    fixture.componentRef.setInput('width', '12rem');
    fixture.componentRef.setInput('height', '2rem');
    expect(placeholder().style.width).toBe('12rem');
    expect(placeholder().style.height).toBe('2rem');
  });

  it('utilise size et arrondit en forme circle', () => {
    fixture.componentRef.setInput('shape', 'circle');
    fixture.componentRef.setInput('size', '3rem');
    expect(placeholder().style.width).toBe('3rem');
    expect(placeholder().style.height).toBe('3rem');
    expect([...placeholder().classList]).toContain('rounded-circle');
  });
});

describe('FormFieldComponent', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<FormFieldComponent>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FormFieldComponent] }).compileComponents();
    fixture = TestBed.createComponent(FormFieldComponent);
  });

  const render = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement;
  };

  it('affiche le libellé et marque les champs requis', () => {
    fixture.componentRef.setInput('label', 'Nom du produit');
    fixture.componentRef.setInput('required', true);
    expect(render().querySelector('label')!.textContent).toContain('Nom du produit');
    expect(render().querySelector('label .text-danger')!.textContent).toBe('*');
  });

  it("affiche le texte d'aide en l'absence d'erreur", () => {
    fixture.componentRef.setInput('hint', 'Trois caractères minimum');
    expect(render().querySelector('.form-field-hint')!.textContent).toContain('Trois caractères minimum');
  });

  it("l'erreur masque le texte d'aide et est annoncée", () => {
    fixture.componentRef.setInput('hint', 'Trois caractères minimum');
    fixture.componentRef.setInput('error', 'Champ obligatoire');
    expect(render().querySelector('.form-field-hint')).toBeNull();
    const error = render().querySelector('.form-field-error')!;
    expect(error.textContent).toContain('Champ obligatoire');
    expect(error.getAttribute('role')).toBe('alert');
  });
});
