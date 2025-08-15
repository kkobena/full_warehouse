export function textColor(documentStyle: CSSStyleDeclaration): string {
  return documentStyle.getPropertyValue('--p-text-color');
}
export function textColorSecondary(documentStyle: CSSStyleDeclaration): string {
  return documentStyle.getPropertyValue('--p-text-muted-color');
}
export function surfaceBorder(documentStyle: CSSStyleDeclaration): string {
  return documentStyle.getPropertyValue('--p-content-border-color');
}
export function backgroundColor(documentStyle: CSSStyleDeclaration): string[] {
  return [
    documentStyle.getPropertyValue('--p-teal-400'),
    documentStyle.getPropertyValue('--p-orange-400'),
   // documentStyle.getPropertyValue('--p-emerald-400'),
    documentStyle.getPropertyValue('--p-fuchsia-400'),

   // documentStyle.getPropertyValue('--p-green-400'),

    documentStyle.getPropertyValue('--p-indigo-400'),
    documentStyle.getPropertyValue('--p-lime-400'),
    documentStyle.getPropertyValue('--p-red-400'),

    documentStyle.getPropertyValue('--p-cyan-400'),
    documentStyle.getPropertyValue('--p---p-primary-color-400'),
    documentStyle.getPropertyValue('--p-amber-400'),
    documentStyle.getPropertyValue('--p-violet-400'),
    documentStyle.getPropertyValue('--p-yellow-400'),
    documentStyle.getPropertyValue('--p-blue-400'),
    documentStyle.getPropertyValue('--p-emerald-400'),
    documentStyle.getPropertyValue('--p-sky-400'),
    documentStyle.getPropertyValue('--p-rose-400'),
    documentStyle.getPropertyValue('--p-purple-400'),
    documentStyle.getPropertyValue('--p-pink-400'),
  ];
}
export function hoverBackgroundColor(documentStyle: CSSStyleDeclaration): string[] {
  return [
    documentStyle.getPropertyValue('--p-teal-300'),
    documentStyle.getPropertyValue('--p-orange-300'),
    documentStyle.getPropertyValue('--p-fuchsia-300'),

    documentStyle.getPropertyValue('--p-indigo-300'),
    documentStyle.getPropertyValue('--p-lime-300'),
    documentStyle.getPropertyValue('--p-red-300'),

    documentStyle.getPropertyValue('--p-cyan-300'),
    documentStyle.getPropertyValue('--p-green-300'),
    documentStyle.getPropertyValue('--p-amber-300'),
    documentStyle.getPropertyValue('--p-violet-300'),
    documentStyle.getPropertyValue('--p-yellow-300'),
    documentStyle.getPropertyValue('--p-blue-300'),
    documentStyle.getPropertyValue('--p-emerald-300'),
    documentStyle.getPropertyValue('--p-sky-300'),
    documentStyle.getPropertyValue('--p-rose-300'),
    documentStyle.getPropertyValue('--p-purple-300'),
    documentStyle.getPropertyValue('--p-pink-300'),
  ];
}
