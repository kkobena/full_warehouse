import { Recette } from './recette.model';
import { ChiffreAffaireAchats } from './chiffre-affaire-achats';
import { MouvementCaisse } from './mouvement-caisse.model';
import { ChiffreAffaireRecord } from './chiffre-affaire-record.model';

export class ChiffreAffaire {
  recettes: Recette[];
  chiffreAffaire: ChiffreAffaireRecord;
  achats: ChiffreAffaireAchats;
  mouvementCaisses: MouvementCaisse[];
}
