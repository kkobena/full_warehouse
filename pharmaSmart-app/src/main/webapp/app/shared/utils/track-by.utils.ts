/**
 * TrackBy utility functions for Angular *ngFor directives
 * These functions improve performance by helping Angular track items in lists
 * 
 * @see https://angular.io/api/common/NgForOf#change-propagation
 */

/**
 * Track items by their 'id' property
 * Use this for lists of objects that have an 'id' field
 * 
 * @example
 * <tr *ngFor="let product of products; trackBy: trackById">
 */
export function trackById<T extends { id: any }>(index: number, item: T): any {
  return item.id;
}

/**
 * Track items by array index
 * Use this for static lists that don't change order
 * 
 * @example
 * <li *ngFor="let item of items; trackBy: trackByIndex">
 */
export function trackByIndex(index: number): number {
  return index;
}

/**
 * Create a trackBy function for a specific property
 * Use this when your objects have a different unique identifier
 * 
 * @example
 * trackBySku = trackByProp<Product>('sku');
 * 
 * <tr *ngFor="let product of products; trackBy: trackBySku">
 */
export function trackByProp<T>(prop: keyof T) {
  return (index: number, item: T): any => item[prop];
}

/**
 * Track items by their 'code' property
 * Common for entities with code identifiers
 * 
 * @example
 * <tr *ngFor="let item of items; trackBy: trackByCode">
 */
export function trackByCode<T extends { code: any }>(index: number, item: T): any {
  return item.code;
}

/**
 * Track items by their 'uuid' property
 * Use for entities with UUID identifiers
 * 
 * @example
 * <tr *ngFor="let item of items; trackBy: trackByUuid">
 */
export function trackByUuid<T extends { uuid: any }>(index: number, item: T): any {
  return item.uuid;
}

/**
 * Create a composite trackBy function using multiple properties
 * Use this when you need to track by multiple fields
 * 
 * @example
 * trackByComposite = trackByMultipleProps<Product>(['id', 'version']);
 * 
 * <tr *ngFor="let product of products; trackBy: trackByComposite">
 */
export function trackByMultipleProps<T>(props: Array<keyof T>) {
  return (index: number, item: T): string => {
    return props.map(prop => item[prop]).join('-');
  };
}
