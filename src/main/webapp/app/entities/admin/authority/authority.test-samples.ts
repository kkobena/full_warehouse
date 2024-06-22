import { IAuthority, NewAuthority } from './authority.model';

export const sampleWithRequiredData: IAuthority = {
  name: '8e8e9cd8-e3a7-4228-b4c2-804d96c6618a',
};

export const sampleWithPartialData: IAuthority = {
  name: '8a3e724d-48bf-45b3-8ec0-7875b013dcc6',
};

export const sampleWithFullData: IAuthority = {
  name: '13133cce-44da-4012-a87a-9bace8b715f2',
};

export const sampleWithNewData: NewAuthority = {
  name: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
