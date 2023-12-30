import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ standalone: true, name: 'findLanguageFromKey' })
export class FindLanguageFromKeyPipe implements PipeTransform {
  private languages: { [key: string]: { name: string; rtl?: boolean } } = {
    en: { name: 'English' },
    fr: { name: 'Français' },
  };

  transform(lang: string): string {
    return this.languages[lang].name;
  }
}
