export class RecapParam {
  usersId?: number[];
  onlyVente: boolean;
  fromDate: string; // Utiliser string pour LocalDate (format ISO)
  toDate: string;
  fromTime?: string; // Utiliser string pour LocalTime (format HH:mm:ss)
  toTime?: string;
}
