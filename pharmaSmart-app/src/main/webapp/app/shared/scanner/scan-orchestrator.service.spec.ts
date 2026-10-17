import { ScanOrchestratorService } from './scan-orchestrator.service';

/** Séparateur de groupe GS1 (ASCII 29). */
const GS = '\u001D';

/**
 * `detectBarcodeType` est un miroir de `DataMatrixParserServiceImpl.detectBarcodeType` (Java).
 * Les cas ci-dessous reprennent ceux de `DataMatrixParserServiceImplTest` : toute divergence
 * entre les deux implémentations se voit ici.
 */
describe('ScanOrchestratorService.detectBarcodeType', () => {
  const detecte = (code: string): string => ScanOrchestratorService.detectBarcodeType(code);

  it.each(['', '   ', 'abcdef', '12345', '123456789', '1234567890123456'])('[%s] est inconnu', code => {
    expect(detecte(code)).toBe('UNKNOWN');
  });

  it('sept chiffres : CIP-7', () => {
    expect(detecte('1234567')).toBe('CIP_7');
  });

  it('huit chiffres à clé juste : EAN-8', () => {
    expect(detecte('96385074')).toBe('EAN_8');
  });

  it('huit chiffres à clé fausse : inconnu', () => {
    expect(detecte('96385075')).toBe('UNKNOWN');
  });

  it('treize chiffres préfixés 340 : CIP-13', () => {
    expect(detecte('3400930000000')).toBe('CIP_13');
  });

  it('treize chiffres à clé juste : EAN-13', () => {
    expect(detecte('4006381333931')).toBe('EAN_13');
  });

  it('treize chiffres à clé fausse et sans préfixe : inconnu', () => {
    expect(detecte('4006381333932')).toBe('UNKNOWN');
  });

  it('quatorze chiffres : GTIN-14', () => {
    expect(detecte('03400930000000')).toBe('GTIN_14');
  });

  it('un EAN-13 débutant par un AI connu reste un EAN-13', () => {
    expect(detecte('0112345678902')).toBe('EAN_13');
  });

  it('un EAN-8 débutant par un AI connu reste un EAN-8', () => {
    expect(detecte('10234563')).toBe('EAN_8');
  });

  it('une trame AI 17 plus date, de même longueur qu’un EAN-8, reste un DataMatrix', () => {
    expect(detecte('17270630')).toBe('DATAMATRIX');
  });

  it('sept chiffres débutant par un AI connu : GTIN tronqué, donc DataMatrix', () => {
    expect(detecte('0134009')).toBe('DATAMATRIX');
  });

  it('un identifiant d’application GS1 prime sur la détection par longueur', () => {
    expect(detecte('0103400930000000')).toBe('DATAMATRIX');
  });

  it('un séparateur de groupe signale un DataMatrix', () => {
    expect(detecte(`10LOT1${GS}21SN1`)).toBe('DATAMATRIX');
  });

  it.each([
    ['<GS>', '10LOT1<GS>21SN1'],
    ['{GS}', '10LOT1{GS}21SN1'],
    ['^029', '10LOT1^02921SN1'],
    ['%1D', '10LOT1%1D21SN1'],
  ])('la notation %s du séparateur est reconnue', (_notation, code) => {
    expect(detecte(code)).toBe('DATAMATRIX');
  });

  it.each([']d2', ']d1', ']C1', ']Q3', ']e0'])('l’identifiant de symbologie %s est retiré', prefixe => {
    expect(detecte(`${prefixe}03400930000000`)).toBe('GTIN_14');
  });

  it('le FNC1 imprimable en tête est retiré', () => {
    expect(detecte('è4006381333931')).toBe('EAN_13');
  });

  it('un séparateur de groupe en tête est retiré', () => {
    expect(detecte(`${GS}4006381333931`)).toBe('EAN_13');
  });

  it('les caractères STX et ETX de transmission sont retirés', () => {
    expect(detecte('\u00024006381333931\u0003')).toBe('EAN_13');
  });

  it('les espaces autour du code sont ignorés', () => {
    expect(detecte('  1234567  ')).toBe('CIP_7');
  });
});
