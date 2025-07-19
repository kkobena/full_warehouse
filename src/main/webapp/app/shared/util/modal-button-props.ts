export const rejectButtonProps = (): any => {
  return {
    label: 'Non',
    icon: 'pi pi-times',
    outlined: true,
    size: 'small',
    severity: 'danger',
  };
};
export const acceptButtonProps = (): any => {
  return {
    label: 'Oui',
    icon: 'pi pi-check',
    outlined: true,
    size: 'small',
    id: 'accept',
    autofocus: true
    /*  severity: 'success', */
  };
};
export const rejectWarningButtonProps = (): any => {
  return {
    label: 'Fermer',
    icon: 'pi pi-times',
    outlined: true,
    size: 'small',
    severity: 'danger',
  };
};
