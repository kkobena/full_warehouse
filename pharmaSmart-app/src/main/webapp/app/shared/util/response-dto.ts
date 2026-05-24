export interface IResponseDto {
  message?: string;
  success?: boolean;
  size?: number;
  totalSize?: number;
  completed?: boolean;
  rejectFileUrl?: string;
  errorSize?: number;
}

export class ResponseDto implements IResponseDto {
  constructor(
    public message?: string,
    public success?: boolean,
    public size?: number,
    public totalSize?: number,
    public completed?: boolean,
    public rejectFileUrl?: string,
    public errorSize?: number,
  ) {}
}
