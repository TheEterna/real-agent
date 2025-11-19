export class Attachment {
  name: string
  size: number
  file: File
  constructor(name: string, size: number, file: File) {
    this.name = name
    this.size = size
    this.file = file
  }
  get sizeKB(): number { return Math.round(this.size / 1024) }
}
