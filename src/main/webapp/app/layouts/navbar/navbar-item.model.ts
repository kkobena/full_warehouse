
import { IconProp } from '@fortawesome/fontawesome-svg-core';


export interface NavItem {
  label: string;
  routerLink?: string;
  authorities?: string[];
  faIcon?: IconProp;
  children?: NavItem[];
  click?:()=> void;
}



