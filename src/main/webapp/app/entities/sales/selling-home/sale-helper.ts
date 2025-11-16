// Enum to manage sale types
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { TranslateService } from '@ngx-translate/core';
import { ICustomer } from '../../../shared/model/customer.model';
import { ISales } from '../../../shared/model/sales.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';

export enum SaleType {
  COMPTANT = 'comptant',
  CARNET = 'carnet',
  ASSURANCE = 'assurance',
}

// Helper function to check if numBon is empty
export function isBonEmpty(element: IClientTiersPayant): boolean {
  return !element.numBon?.trim();
}

// Helper to determine if the current mode is an edit
export function isEditMode(mode: string): boolean {
  return mode === 'edit';
}

// Translates a label in sales context
export function translateSalesLabel(translate: TranslateService, key: string): string {
  return translate.instant(`warehouseApp.sales.${key}`);
}

// Safely extract and assign customer data to a sale
export function assignCustomerToSale(sale: ISales, customer: ICustomer): void {
  sale.customerId = customer.id;
  sale.customer = customer;
  sale.tiersPayants = customer.tiersPayants;
}

// Determines if the sale qualifies as a VO sale
export function isVoSaleType(active: string): boolean {
  return active === SaleType.ASSURANCE || active === SaleType.CARNET;
}

export function isVno(type: string): boolean {
  return type === 'VNO';
}

export function isVo(type: string): boolean {
  return type === 'VO';
}

// Determines the active tab based on sale type and nature
export function getActiveTab(sale: ISales): SaleType {
  if (sale?.type === 'VNO') {
    return SaleType.COMPTANT;
  }
  if (sale?.type === 'VO') {
    return sale.natureVente === 'CARNET' ? SaleType.CARNET : SaleType.ASSURANCE;
  }
  return SaleType.COMPTANT;
}

// Used in confirmation dialog switch logic
export function getNavChangeMessage(nextId: string, translate: TranslateService): string {
  switch (nextId) {
    case SaleType.COMPTANT:
      return translateSalesLabel(translate, 'modificationTypeToComptant');
    case SaleType.ASSURANCE:
    case SaleType.CARNET:
      return translateSalesLabel(translate, 'modificationTypeToVo');
    default:
      return '';
  }
}

// Common error handler to display dialog
export function showCommonError(modalService: NgbModal, message: string, infoClass = 'alert alert-danger'): void {
  const modalRef = modalService.open(AlertInfoComponent, {
    backdrop: 'static',
    centered: true,
  });
  modalRef.componentInstance.message = message;
  modalRef.componentInstance.infoClass = infoClass;
}

export function showCommonModal<T>(
  modalService: NgbModal,
  component: new (...args: any[]) => T,
  componentInputs: Partial<T>,
  onClose?: (reason: any) => void,
  size?: string,
  modalDialogClass?: string,
  onDismiss?: (dismis: any) => void,
): void {
  const modalRef = modalService.open(component, {
    backdrop: 'static',
    centered: true,
    size: size || 'lg',
    modalDialogClass,
  });

  Object.assign(modalRef.componentInstance, componentInputs);

  if (onClose || onDismiss) {
    modalRef.result.then(onClose, onDismiss);
  }
}
