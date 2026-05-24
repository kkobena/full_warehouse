import { IUser } from '../../core/user/user.model';

export class UtilisationCleSecurite {
  cleSecuriteOwner?: IUser;
  connectedUser?: IUser;
  caisse?: string;
  privilege?: string;
  mvtDate?: Date;
  entityId: number;
  entityName?: string;
  actionAuthorityKey?: string;
  commentaire?: string;
}
