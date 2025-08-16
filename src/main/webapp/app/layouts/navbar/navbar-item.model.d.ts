type NavbarItem = {
  name: string;
  route: string;
  translationKey: string;
};

export default NavbarItem;
import { IconProp } from '@fortawesome/fontawesome-svg-core';

/**
 * Defines the structure for a single navigation item.
 * This can represent a top-level dropdown menu or a link within it.
 */
export interface NavItem {
  /**
   * The translation key used to display the item's text.
   * Example: 'global.menu.admin.main'
   */
  translationKey: string;

  /**
   * The route to navigate to when the item is clicked.
   * Required for clickable links, optional for parent dropdown items.
   * Example: '/admin/user-management'
   */
  routerLink?: string;

  /**
   * An array of authorities. The user must have at least one of these
   * to see the item. If undefined, the item is visible to all
   * authenticated users.
   */
  authorities?: string[];

  /**
   * The Font Awesome icon to display.
   * Example: 'users-cog'
   */
  faIcon?: IconProp;

  /**
   * An array of child NavItem objects for creating a nested dropdown menu.
   */
  children?: NavItem[];
}



