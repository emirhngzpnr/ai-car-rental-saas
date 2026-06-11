export type TenantSettingDataType = 'STRING' | 'INTEGER' | 'BOOLEAN' | 'DECIMAL';

export interface TenantSettingResponse {
  id: number;
  tenantId: number;
  settingKey: string;
  settingValue: string;
  dataType: TenantSettingDataType;
  defaultValue: string;
  description: string;
  active: boolean;
}

export interface UpdateTenantSettingRequest {
  settingValue: string;
}
