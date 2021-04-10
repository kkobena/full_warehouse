export interface IDailyca {
  amount?: number;
  numberTransaction?: number;
}

export class Dailyca implements IDailyca {
  constructor(public amount?: number, public numberTransaction?: number) {}
}
