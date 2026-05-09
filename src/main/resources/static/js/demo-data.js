export const demoCompanies = [
  { ico: '00006947', name: 'Ministerstvo financi' },
  { ico: '26168685', name: 'Seznam.cz' },
  { ico: '27082440', name: 'Alza.cz' },
  { ico: '45274649', name: 'CEZ' }
];

export const sampleJson = JSON.stringify([
  {
    name: 'Atlas Data Lab s.r.o.',
    registrationNumber: '70010001',
    country: 'CZ',
    legalForm: 's.r.o.',
    address: 'Na Prikope 12, Praha',
    people: [
      { fullName: 'Jan Novak', role: 'jednatel' },
      { fullName: 'Eva Svobodova', role: 'datova analyticka' }
    ]
  },
  {
    name: 'North Bridge Ventures a.s.',
    registrationNumber: '70010002',
    country: 'CZ',
    legalForm: 'a.s.',
    address: 'Jana Babaka 11, Brno',
    people: [
      { fullName: 'Petra Dvorakova', role: 'clen predstavenstva' },
      { fullName: 'Tomas Marek', role: 'financni reditel' }
    ]
  }
], null, 2);

export const sampleCsv = [
  'name,registrationNumber,country,legalForm,people',
  'Data Bridge s.r.o.,70020001,CZ,s.r.o.,Michaela Cerna|jednatel;Karel Novak|analytik',
  'Meridian Trade a.s.,70020002,CZ,a.s.,Lucie Hruba|clen predstavenstva;Pavel Urban|obchodni reditel'
].join('\n');
