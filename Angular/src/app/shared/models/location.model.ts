export interface LocationRequest {
  country: string;
  level1: string;
  level2: string;
  level3: string;
  level4: string;
  streetAddress: string;
  postalCode?: string;
  apartment?: string;
}

export interface LocationResponse extends LocationRequest {
  id: number;
}

export interface HierarchyLabel {
  level1: string;
  level2: string;
  level3: string;
  level4: string;
}

export interface GeoNodeDto {
  id: number;
  name: string;
  type: string;
  code: string;
}

export const countryNames: Record<string, string> = {
  'BD': 'Bangladesh', 'IN': 'India', 'PK': 'Pakistan', 'LK': 'Sri Lanka', 'NP': 'Nepal', 'MV': 'Maldives', 'AF': 'Afghanistan',
  'CN': 'China', 'JP': 'Japan', 'KR': 'South Korea', 'ID': 'Indonesia', 'MY': 'Malaysia', 'PH': 'Philippines', 'SG': 'Singapore', 'TH': 'Thailand', 'VN': 'Vietnam', 'TW': 'Taiwan', 'MM': 'Myanmar', 'KH': 'Cambodia',
  'US': 'United States', 'CA': 'Canada', 'MX': 'Mexico', 'BR': 'Brazil', 'AR': 'Argentina', 'CO': 'Colombia', 'CL': 'Chile', 'PE': 'Peru', 'VE': 'Venezuela', 'EC': 'Ecuador', 'CU': 'Cuba', 'BO': 'Bolivia', 'PY': 'Paraguay', 'UY': 'Uruguay', 'CR': 'Costa Rica', 'PA': 'Panama', 'SV': 'El Salvador', 'GT': 'Guatemala', 'HN': 'Honduras', 'DO': 'Dominican Republic',
  'GB': 'United Kingdom', 'DE': 'Germany', 'FR': 'France', 'IT': 'Italy', 'ES': 'Spain', 'RU': 'Russia', 'UA': 'Ukraine', 'PL': 'Poland', 'NL': 'Netherlands', 'BE': 'Belgium', 'SE': 'Sweden', 'CH': 'Switzerland', 'AT': 'Austria', 'PT': 'Portugal', 'GR': 'Greece', 'CZ': 'Czech Republic', 'RO': 'Romania', 'HU': 'Hungary', 'IE': 'Ireland', 'DK': 'Denmark', 'FI': 'Finland', 'NO': 'Norway', 'TR': 'Turkey', 'BY': 'Belarus', 'RS': 'Serbia', 'BG': 'Bulgaria', 'SK': 'Slovakia', 'HR': 'Croatia', 'LT': 'Lithuania', 'LV': 'Latvia',
  'AE': 'United Arab Emirates', 'SA': 'Saudi Arabia', 'EG': 'Egypt', 'IR': 'Iran', 'IL': 'Israel', 'IQ': 'Iraq', 'DZ': 'Algeria', 'MA': 'Morocco', 'QA': 'Qatar', 'KW': 'Kuwait', 'OM': 'Oman', 'LB': 'Lebanon', 'JO': 'Jordan', 'TN': 'Tunisia',
  'ZA': 'South Africa', 'NG': 'Nigeria', 'KE': 'Kenya', 'ET': 'Ethiopia', 'GH': 'Ghana', 'TZ': 'Tanzania', 'UG': 'Uganda', 'CI': 'Ivory Coast', 'CM': 'Cameroon', 'AO': 'Angola', 'ZM': 'Zambia', 'ZW': 'Zimbabwe', 'SN': 'Senegal', 'RW': 'Rwanda',
  'AU': 'Australia', 'NZ': 'New Zealand', 'FJ': 'Fiji'
};

export const globalHierarchyLabels: Record<string, HierarchyLabel> = {
  // ==========================================
  // SOUTH ASIA
  // ==========================================
  'BD': { level1: 'Division', level2: 'District', level3: 'Upazila / Municipality', level4: 'Police Station' },
  'IN': { level1: 'State / Union Territory', level2: 'District', level3: 'Sub-district / Tehsil', level4: 'Police Station' },
  'PK': { level1: 'Province / Territory', level2: 'District', level3: 'Tehsil / Town', level4: 'Police Station' },
  'LK': { level1: 'Province', level2: 'District', level3: 'Divisional Secretariat', level4: 'Police Station' },
  'NP': { level1: 'Province', level2: 'District', level3: 'Municipality', level4: 'Ward / Station' },
  'MV': { level1: 'Atoll', level2: 'Island', level3: 'City / Ward', level4: 'Police Station' },
  'AF': { level1: 'Province (Wilayat)', level2: 'District (Wuleswali)', level3: 'City / Municipality', level4: 'Police District' },

  // ==========================================
  // SOUTHEAST & EAST ASIA
  // ==========================================
  'CN': { level1: 'Province', level2: 'Prefecture', level3: 'County / District', level4: 'Township / Police Station' },
  'JP': { level1: 'Prefecture', level2: 'Subprefecture', level3: 'City / Ward', level4: 'Police Box (Koban)' },
  'KR': { level1: 'Province', level2: 'City / County', level3: 'District (Gu)', level4: 'Neighborhood (Dong)' },
  'ID': { level1: 'Province', level2: 'Regency / City', level3: 'District (Kecamatan)', level4: 'Village / Police Sector' },
  'MY': { level1: 'State', level2: 'District', level3: 'Mukim (Commune)', level4: 'Police Station' },
  'PH': { level1: 'Region', level2: 'Province', level3: 'City / Municipality', level4: 'Barangay / Precinct' },
  'SG': { level1: 'Region', level2: 'Planning Area', level3: 'Subzone', level4: 'Police Division' },
  'TH': { level1: 'Province', level2: 'District (Amphoe)', level3: 'Sub-district (Tambon)', level4: 'Village / Station' },
  'VN': { level1: 'Province', level2: 'District', level3: 'Commune / Ward', level4: 'Police Post' },
  'TW': { level1: 'Special Municipality / County', level2: 'District / City', level3: 'Village', level4: 'Neighborhood' },
  'MM': { level1: 'State / Region', level2: 'District', level3: 'Township', level4: 'Ward / Village' },
  'KH': { level1: 'Province', level2: 'District', level3: 'Commune', level4: 'Village' },

  // ==========================================
  // THE AMERICAS (NORTH & SOUTH)
  // ==========================================
  'US': { level1: 'State', level2: 'County', level3: 'City / Township', level4: 'Police Precinct / Station' },
  'CA': { level1: 'Province / Territory', level2: 'Regional Municipality', level3: 'City / Town', level4: 'Police Division' },
  'MX': { level1: 'State', level2: 'Municipality', level3: 'City', level4: 'Neighborhood (Colonia)' },
  'BR': { level1: 'State', level2: 'Mesoregion', level3: 'Microregion', level4: 'Municipality / District' },
  'AR': { level1: 'Province', level2: 'Department', level3: 'Municipality', level4: 'Police Station' },
  'CO': { level1: 'Department', level2: 'Province', level3: 'Municipality', level4: 'Comuna / Corregimiento' },
  'CL': { level1: 'Region', level2: 'Province', level3: 'Commune', level4: 'Police Unit (Comisaría)' },
  'PE': { level1: 'Region', level2: 'Province', level3: 'District', level4: 'Police Station' },
  'VE': { level1: 'State', level2: 'Municipality', level3: 'Parish', level4: 'Police Sector' },
  'EC': { level1: 'Province', level2: 'Canton', level3: 'Parish', level4: 'Neighborhood' },
  'CU': { level1: 'Province', level2: 'Municipality', level3: 'Ward', level4: 'Police Station' },
  'BO': { level1: 'Department', level2: 'Province', level3: 'Municipality', level4: 'Canton' },
  'PY': { level1: 'Department', level2: 'District', level3: 'Municipality', level4: 'Neighborhood' },
  'UY': { level1: 'Department', level2: 'Municipality', level3: 'City / Town', level4: 'Police Precinct' },
  'CR': { level1: 'Province', level2: 'Canton', level3: 'District', level4: 'Neighborhood' },
  'PA': { level1: 'Province', level2: 'District', level3: 'Corregimiento', level4: 'Police Zone' },
  'SV': { level1: 'Department', level2: 'District', level3: 'Municipality', level4: 'Police Post' },
  'GT': { level1: 'Department', level2: 'Municipality', level3: 'Village', level4: 'Police Station' },
  'HN': { level1: 'Department', level2: 'Municipality', level3: 'Village / Hamlet', level4: 'Police Unit' },
  'DO': { level1: 'Province', level2: 'Municipality', level3: 'Municipal District', level4: 'Section / Barrio' },

  // ==========================================
  // EUROPE
  // ==========================================
  'GB': { level1: 'Country (e.g., England)', level2: 'County', level3: 'City / Town', level4: 'Police Constabulary' },
  'DE': { level1: 'State (Bundesland)', level2: 'District (Kreis)', level3: 'Municipality', level4: 'Police Station' },
  'FR': { level1: 'Region', level2: 'Department', level3: 'Arrondissement', level4: 'Commune / Police Station' },
  'IT': { level1: 'Region', level2: 'Province', level3: 'Municipality', level4: 'Questura / Station' },
  'ES': { level1: 'Autonomous Community', level2: 'Province', level3: 'Comarca', level4: 'Municipality' },
  'RU': { level1: 'Republic / Oblast', level2: 'District (Raion)', level3: 'City / Town', level4: 'Police Department' },
  'UA': { level1: 'Oblast', level2: 'Raion', level3: 'City / Hromada', level4: 'Police Station' },
  'PL': { level1: 'Voivodeship', level2: 'County (Powiat)', level3: 'Commune (Gmina)', level4: 'Police Precinct' },
  'NL': { level1: 'Province', level2: 'COROP Region', level3: 'Municipality', level4: 'Police Unit' },
  'BE': { level1: 'Region', level2: 'Province', level3: 'Arrondissement', level4: 'Municipality / Police Zone' },
  'SE': { level1: 'County', level2: 'Municipality', level3: 'City / Town', level4: 'Police District' },
  'CH': { level1: 'Canton', level2: 'District', level3: 'Municipality', level4: 'Police Post' },
  'AT': { level1: 'State', level2: 'District', level3: 'Municipality', level4: 'Police Inspectorate' },
  'PT': { level1: 'District', level2: 'Municipality', level3: 'Parish (Freguesia)', level4: 'Police Station' },
  'GR': { level1: 'Region', level2: 'Regional Unit', level3: 'Municipality', level4: 'Police Department' },
  'CZ': { level1: 'Region', level2: 'District', level3: 'Municipality', level4: 'Police Station' },
  'RO': { level1: 'County', level2: 'City / Town', level3: 'Commune', level4: 'Police Section' },
  'HU': { level1: 'County', level2: 'District', level3: 'Municipality', level4: 'Police Captaincy' },
  'IE': { level1: 'Province', level2: 'County', level3: 'City / Town', level4: 'Garda Station' },
  'DK': { level1: 'Region', level2: 'Municipality', level3: 'City', level4: 'Police District' },
  'FI': { level1: 'Region', level2: 'Sub-region', level3: 'Municipality', level4: 'Police Department' },
  'NO': { level1: 'County', level2: 'Municipality', level3: 'City', level4: 'Police District' },
  'TR': { level1: 'Province (İl)', level2: 'District (İlçe)', level3: 'Municipality', level4: 'Neighborhood (Mahalle)' },
  'BY': { level1: 'Oblast', level2: 'Raion', level3: 'City / Town', level4: 'Police Station' },
  'RS': { level1: 'District', level2: 'Municipality', level3: 'City / Village', level4: 'Police Station' },
  'BG': { level1: 'Province', level2: 'Municipality', level3: 'City / Village', level4: 'Police Station' },
  'SK': { level1: 'Region', level2: 'District', level3: 'Municipality', level4: 'Police Station' },
  'HR': { level1: 'County', level2: 'City / Town', level3: 'Municipality', level4: 'Police Station' },
  'LT': { level1: 'County', level2: 'Municipality', level3: 'Eldership', level4: 'Police Commissariat' },
  'LV': { level1: 'Region', level2: 'Municipality', level3: 'City / Parish', level4: 'Police Station' },

  // ==========================================
  // MIDDLE EAST & NORTH AFRICA
  // ==========================================
  'AE': { level1: 'Emirate', level2: 'Region', level3: 'City / Area', level4: 'Police Station' },
  'SA': { level1: 'Province', level2: 'Governorate', level3: 'Sub-Governorate', level4: 'Police Center' },
  'EG': { level1: 'Governorate', level2: 'Markaz (Region)', level3: 'City', level4: 'District / Station' },
  'IR': { level1: 'Province (Ostan)', level2: 'County (Shahrestan)', level3: 'District (Bakhsh)', level4: 'City / Rural District' },
  'IL': { level1: 'District', level2: 'Sub-district', level3: 'City / Local Council', level4: 'Police Station' },
  'IQ': { level1: 'Governorate', level2: 'District', level3: 'Sub-district', level4: 'Police Station' },
  'DZ': { level1: 'Province (Wilaya)', level2: 'District (Daïra)', level3: 'Commune', level4: 'Police Station' },
  'MA': { level1: 'Region', level2: 'Prefecture / Province', level3: 'Arrondissement / Commune', level4: 'Police District' },
  'QA': { level1: 'Municipality', level2: 'Zone', level3: 'District', level4: 'Police Department' },
  'KW': { level1: 'Governorate', level2: 'Area', level3: 'Block', level4: 'Police Station' },
  'OM': { level1: 'Governorate', level2: 'Wilayat', level3: 'City / Village', level4: 'Police Station' },
  'LB': { level1: 'Governorate', level2: 'District', level3: 'Municipality', level4: 'Police Station' },
  'JO': { level1: 'Governorate', level2: 'District', level3: 'Sub-district', level4: 'Police Station' },
  'TN': { level1: 'Governorate', level2: 'Delegation', level3: 'Sector (Imada)', level4: 'Police Station' },

  // ==========================================
  // SUB-SAHARAN AFRICA
  // ==========================================
  'ZA': { level1: 'Province', level2: 'District Municipality', level3: 'Local Municipality', level4: 'Police Ward' },
  'NG': { level1: 'State', level2: 'Local Government Area (LGA)', level3: 'Ward', level4: 'Police Command' },
  'KE': { level1: 'County', level2: 'Sub-county', level3: 'Ward', level4: 'Police Division / Station' },
  'ET': { level1: 'Regional State', level2: 'Zone', level3: 'Woreda (District)', level4: 'Kebele (Ward)' },
  'GH': { level1: 'Region', level2: 'District', level3: 'Metropolis / Municipality', level4: 'Police Station' },
  'TZ': { level1: 'Region', level2: 'District', level3: 'Ward', level4: 'Village / Police Station' },
  'UG': { level1: 'Region', level2: 'District', level3: 'County / Sub-county', level4: 'Parish / Police Post' },
  'CI': { level1: 'District', level2: 'Region', level3: 'Department', level4: 'Sub-prefecture / Commune' },
  'CM': { level1: 'Region', level2: 'Division', level3: 'Sub-division', level4: 'Police District' },
  'AO': { level1: 'Province', level2: 'Municipality', level3: 'Commune', level4: 'Police Station' },
  'ZM': { level1: 'Province', level2: 'District', level3: 'Constituency', level4: 'Ward / Police Camp' },
  'ZW': { level1: 'Province', level2: 'District', level3: 'Ward', level4: 'Police Station' },
  'SN': { level1: 'Region', level2: 'Department', level3: 'Arrondissement', level4: 'Commune' },
  'RW': { level1: 'Province', level2: 'District', level3: 'Sector', level4: 'Cell / Village' },

  // ==========================================
  // OCEANIA
  // ==========================================
  'AU': { level1: 'State / Territory', level2: 'Local Gov. Area (LGA)', level3: 'Suburb / Town', level4: 'Police District / Station' },
  'NZ': { level1: 'Region', level2: 'Territorial Authority', level3: 'Suburb', level4: 'Police Station' },
  'FJ': { level1: 'Division', level2: 'Province', level3: 'District', level4: 'Police Station' }
};
