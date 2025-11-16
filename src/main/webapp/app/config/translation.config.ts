import { MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';

export const translationNotFoundMessage = 'translation-not-found';

export class CustomMissingTranslationHandler implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): string {
    const { key } = params;
    return `${translationNotFoundMessage}[${key}]`;
  }
}
