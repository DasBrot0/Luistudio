export type CurrentRoomStatus = 'LIBRE' | 'OCUPADA' | 'MANTENIMIENTO'
export interface CampusMapRoom { id:number; code:string; name:string; venue:string; capacity:number; status:CurrentRoomStatus; withinSchedule:boolean; reservableNow:boolean }
export interface CampusMapLocation { name:string; rooms:CampusMapRoom[] }
export interface CampusMapPavilion { id:number; code:string; name:string; latitude:number|null; longitude:number|null; aggregateStatus:'CON_DISPONIBILIDAD'|'SIN_DISPONIBILIDAD'|'MANTENIMIENTO_TOTAL'|'SIN_ESPACIOS'; summary:{free:number;occupied:number;maintenance:number;total:number}; locations:CampusMapLocation[] }
export interface CampusMapCampus { code:string; name:string; center:{latitude:number|null;longitude:number|null}; defaultZoom:number; pavilions:CampusMapPavilion[] }
export interface CampusMapResponse { generatedAt:string; refreshAfterSeconds:number; campuses:CampusMapCampus[] }
