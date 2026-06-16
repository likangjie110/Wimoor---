<template>
  <div>
    <input ref="excel-upload-input" class="excel-upload-input" type="file" accept=".xlsx,.csv" @change="handleClick">
    <div class="drop" @drop="handleDrop" @dragover="handleDragover" @dragenter="handleDragover">
      Drop excel file here or
      <el-button :loading="loading" style="margin-left:16px;" size="mini" type="primary" @click="handleUpload">
        Browse
      </el-button>
    </div>
  </div>
</template>

<script>
import readXlsxFile from 'read-excel-file'

export default {
  props: {
    beforeUpload: Function, // eslint-disable-line
    onSuccess: Function// eslint-disable-line
  },
  data() {
    return {
      loading: false,
      excelData: {
        header: null,
        results: null
      }
    }
  },
  methods: {
    generateData({ header, results }) {
      this.excelData.header = header
      this.excelData.results = results
      this.onSuccess && this.onSuccess(this.excelData)
    },
    handleDrop(e) {
      e.stopPropagation()
      e.preventDefault()
      if (this.loading) return
      const files = e.dataTransfer.files
      if (files.length !== 1) {
        this.$message.error('Only support uploading one file!')
        return
      }
      const rawFile = files[0] // only use files[0]

      if (!this.isExcel(rawFile)) {
        this.$message.error('Only supports upload .xlsx, .csv suffix files')
        return false
      }
      this.upload(rawFile)
      e.stopPropagation()
      e.preventDefault()
    },
    handleDragover(e) {
      e.stopPropagation()
      e.preventDefault()
      e.dataTransfer.dropEffect = 'copy'
    },
    handleUpload() {
      this.$refs['excel-upload-input'].click()
    },
    handleClick(e) {
      const files = e.target.files
      const rawFile = files[0] // only use files[0]
      if (!rawFile) return
      this.upload(rawFile)
    },
    upload(rawFile) {
      this.$refs['excel-upload-input'].value = null // fix can't select the same excel

      if (!this.beforeUpload) {
        this.readerData(rawFile)
        return
      }
      const before = this.beforeUpload(rawFile)
      if (before) {
        this.readerData(rawFile)
      }
    },
    readerData(rawFile) {
      this.loading = true
      return new Promise((resolve, reject) => {
        const extension = this.getExtension(rawFile.name)
        const onSuccess = ({ header, results }) => {
          this.generateData({ header, results })
          this.loading = false
          resolve()
        }
        const onError = (error) => {
          this.loading = false
          reject(error)
        }

        if (extension === 'csv') {
          rawFile.text().then((text) => {
            onSuccess(this.parseCsv(text))
          }).catch(onError)
          return
        }

        if (extension === 'xlsx') {
          readXlsxFile(rawFile).then((rows) => {
            onSuccess(this.normalizeRows(rows))
          }).catch(onError)
          return
        }

        this.loading = false
        this.$message.error('暂不支持 .xls 文件，请先转换为 .xlsx 或 .csv')
        reject(new Error('Unsupported excel format'))
      })
    },
    normalizeRows(rows) {
      const normalizedRows = Array.isArray(rows) ? rows : []
      if (normalizedRows.length === 0) {
        return { header: [], results: [] }
      }
      const header = (normalizedRows[0] || []).map((value, index) => this.normalizeHeaderValue(value, index))
      const results = normalizedRows.slice(1).map((row) => this.rowToObject(header, row))
      return { header, results }
    },
    parseCsv(text) {
      const lines = (text || '').replace(/^\uFEFF/, '').split(/\r?\n/).filter((line) => line.trim() !== '')
      if (lines.length === 0) {
        return { header: [], results: [] }
      }
      const matrix = lines.map((line) => this.parseCsvLine(line))
      return this.normalizeRows(matrix)
    },
    parseCsvLine(line) {
      const result = []
      let current = ''
      let inQuotes = false
      for (let index = 0; index < line.length; index += 1) {
        const char = line[index]
        const nextChar = line[index + 1]
        if (char === '"') {
          if (inQuotes && nextChar === '"') {
            current += '"'
            index += 1
          } else {
            inQuotes = !inQuotes
          }
          continue
        }
        if (char === ',' && !inQuotes) {
          result.push(current)
          current = ''
          continue
        }
        current += char
      }
      result.push(current)
      return result
    },
    rowToObject(header, row) {
      return header.reduce((result, key, index) => {
        result[key] = row?.[index] ?? null
        return result
      }, {})
    },
    normalizeHeaderValue(value, index) {
      const text = value == null ? '' : String(value).trim()
      return text || `UNKNOWN ${index}`
    },
    getExtension(fileName) {
      const matched = /\.(xlsx|xls|csv)$/i.exec(fileName || '')
      return matched ? matched[1].toLowerCase() : ''
    },
    isExcel(file) {
      return /\.(xlsx|csv)$/i.test(file.name)
    }
  }
}
</script>

<style scoped>
.excel-upload-input{
  display: none;
  z-index: -9999;
}
.drop{
  border: 2px dashed #bbb;
  width: 600px;
  height: 160px;
  line-height: 160px;
  margin: 0 auto;
  font-size: 24px;
  border-radius: 5px;
  text-align: center;
  color: #bbb;
  position: relative;
}
</style>
