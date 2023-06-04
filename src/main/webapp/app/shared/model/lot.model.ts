export interface ILot {
  id?: number;
  commandeId?: number;
  receiptItemId?: number;
  quantity?: number;
  numLot?: string;
  receiptRefernce?: string;
  createdDate?: string;
  manufacturingDate?: string;
  expiryDate?: string;
  ugQuantityReceived?: number;
  quantityReceived?: number;
}

export class Lot implements ILot {
  constructor(
    public id?: number,
    public commandeId?: number,
    public receiptItemQuantityRequested?: number,
    public receiptItemId?: number,
    public quantity?: number,
    public numLot?: string,
    public receiptRefernce?: string,
    public createdDate?: string,
    public manufacturingDate?: string,
    public expiryDate?: string,
    public ugQuantityReceived?: number,
    public quantityReceived?: number
  ) {}
}
