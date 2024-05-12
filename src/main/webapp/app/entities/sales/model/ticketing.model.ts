export interface ITicketing {
  id?: number;
  cashRegisterId?: number;
  numbernumberOf10Thousand?: number;
  numberOf5Thousand?: number;
  numberOf2Thousand?: number;
  numberOf1Thousand?: number;
  numberOf500Hundred?: number;
  numberOf200Hundred?: number;
  numberOf100Hundred?: number;
  numberOf50?: number;
  numberOf25?: number;
  numberOf10?: number;
  numberOf5?: number;
  numberOf1?: number;
  otherAmount?: number;
}

export class Ticketing implements ITicketing {
  constructor(
    public id?: number,
    public cashRegisterId?: number,
    public numbernumberOf10Thousand?: number,
    public numberOf5Thousand?: number,
    public numberOf2Thousand?: number,
    public numberOf1Thousand?: number,
    public numberOf500Hundred?: number,
    public numberOf200Hundred?: number,
    public numberOf100Hundred?: number,
    public numberOf50?: number,
    public numberOf25?: number,
    public numberOf10?: number,
    public numberOf5?: number,
    public numberOf1?: number,
    public otherAmount?: number,
  ) {}
}
