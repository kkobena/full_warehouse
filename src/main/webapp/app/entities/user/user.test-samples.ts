import { IUser } from './user.model';

export const sampleWithRequiredData: IUser = {
  id: 20819,
  login: 'DCpW.t',
};

export const sampleWithPartialData: IUser = {
  id: 5062,
  login: 'duiPH@1fSTz',
};

export const sampleWithFullData: IUser = {
  id: 6570,
  login: 'rUqZ',
};
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
