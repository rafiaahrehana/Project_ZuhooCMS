export interface CustomRole {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  systemRole: boolean;
}

export interface CustomRoleRequest {
  name: string;
  description?: string;
}

export interface Permission {
  id: number;
  code: string;
  name: string;
  description?: string;
  module: string;
}
