/**
 * 颜色生成工具类
 * 提供随机颜色生成功能，特别是浅色（玻璃色）效果
 */

// HSL颜色接口
interface HSLColor {
  h: number // 色相 0-360
  s: number // 饱和度 0-100
  l: number // 亮度 0-100
}

// RGBA颜色接口
interface RGBAColor {
  r: number // 红色分量 0-255
  g: number // 绿色分量 0-255
  b: number // 蓝色分量 0-255
  a: number // 透明度 0-1
}

/**
 * 生成随机浅色（玻璃色）
 * @param alpha 透明度，默认为0.1-0.3之间的随机值
 * @param saturation 饱和度范围，默认20-60
 * @param lightness 亮度范围，默认70-90
 * @returns CSS颜色字符串 (rgba格式)
 */
export function generateGlassColor(
  alpha?: number,
  saturation: [number, number] = [20, 60],
  lightness: [number, number] = [70, 90]
): string {
  // 随机色相 (0-360度)
  const hue = Math.floor(Math.random() * 360)

  // 随机饱和度 (在指定范围内)
  const sat = Math.floor(Math.random() * (saturation[1] - saturation[0] + 1)) + saturation[0]

  // 随机亮度 (在指定范围内)
  const light = Math.floor(Math.random() * (lightness[1] - lightness[0] + 1)) + lightness[0]

  // 随机透明度 (如果未指定)
  const finalAlpha = alpha ?? (Math.random() * 0.2 + 0.1) // 0.1-0.3

  // 转换HSL为RGB
  const rgb = hslToRgb(hue, sat, light)

  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${finalAlpha})`
}

/**
 * 生成随机浅色（带边框效果）
 * @param borderAlpha 边框透明度，默认为0.3-0.5之间的随机值
 * @returns 包含背景色和边框色的对象
 */
export function generateGlassColorWithBorder(borderAlpha?: number) {
  const baseAlpha = Math.random() * 0.2 + 0.1 // 背景透明度 0.1-0.3
  const finalBorderAlpha = borderAlpha ?? (Math.random() * 0.2 + 0.3) // 边框透明度 0.3-0.5

  // 生成相同色相的颜色
  const hue = Math.floor(Math.random() * 360)
  const sat = Math.floor(Math.random() * 40 + 20) // 20-60
  const light = Math.floor(Math.random() * 20 + 70) // 70-90

  const rgb = hslToRgb(hue, sat, light)

  return {
    background: `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${baseAlpha})`,
    border: `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${finalBorderAlpha})`
  }
}

/**
 * 生成预定义主题色的浅色版本
 * @param themeColor 主题色 (hex格式，如 '#3B82F6')
 * @param alpha 透明度，默认0.15
 * @returns CSS颜色字符串 (rgba格式)
 */
export function generateThemeGlassColor(themeColor: string, alpha: number = 0.15): string {
  const rgb = hexToRgb(themeColor)
  if (!rgb) {
    throw new Error('Invalid hex color format')
  }

  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`
}

/**
 * 获取常用的玻璃色彩色板
 * @returns 预定义的玻璃色数组
 */
export function getGlassColorPalette(): string[] {
  const themes = [
    '#3B82F6', // 蓝色
    '#8B5CF6', // 紫色
    '#06B6D4', // 青色
    '#10B981', // 绿色
    '#F59E0B', // 黄色
    '#EF4444', // 红色
    '#EC4899', // 粉色
    '#84CC16', // 柠檬绿
  ]

  return themes.map(color => generateThemeGlassColor(color, 0.12))
}

/**
 * HSL转RGB
 * @param h 色相 (0-360)
 * @param s 饱和度 (0-100)
 * @param l 亮度 (0-100)
 * @returns RGB颜色对象
 */
function hslToRgb(h: number, s: number, l: number): RGBAColor {
  h /= 360
  s /= 100
  l /= 100

  const a = s * Math.min(l, 1 - l)
  const f = (n: number) => {
    const k = (n + h / (1 / 12)) % 12
    return l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1)
  }

  return {
    r: Math.round(f(0) * 255),
    g: Math.round(f(8) * 255),
    b: Math.round(f(4) * 255),
    a: 1
  }
}

/**
 * Hex转RGB
 * @param hex hex颜色字符串
 * @returns RGB颜色对象或null
 */
function hexToRgb(hex: string): RGBAColor | null {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16),
    a: 1
  } : null
}

/**
 * 生成适用于Tooltip的颜色（更深一些，确保文字可读性）
 * @param alpha 透明度，默认为0.85-0.95之间的随机值
 * @param saturation 饱和度范围，默认30-70
 * @param lightness 亮度范围，默认25-45（较深）
 * @returns CSS颜色字符串 (rgba格式)
 */
export function generateTooltipColor(
  alpha?: number,
  saturation: [number, number] = [30, 70],
  lightness: [number, number] = [25, 45]
): string {
  // 随机色相 (0-360度)
  const hue = Math.floor(Math.random() * 360)

  // 随机饱和度 (在指定范围内)
  const sat = Math.floor(Math.random() * (saturation[1] - saturation[0] + 1)) + saturation[0]

  // 随机亮度 (较深的范围)
  const light = Math.floor(Math.random() * (lightness[1] - lightness[0] + 1)) + lightness[0]

  // 较高的不透明度确保背景足够深
  const finalAlpha = alpha ?? (Math.random() * 0.1 + 0.85) // 0.85-0.95

  // 转换HSL为RGB
  const rgb = hslToRgb(hue, sat, light)

  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${finalAlpha})`
}

/**
 * 随机选择一个玻璃色
 * @returns 随机的玻璃色字符串
 */
export function getRandomGlassColor(): string {
  const palette = getGlassColorPalette()
  const randomColor = generateGlassColor()

  // 70%概率返回预定义颜色，30%概率返回完全随机颜色
  return Math.random() < 0.7
    ? palette[Math.floor(Math.random() * palette.length)]
    : randomColor
}

/**
 * 随机选择一个适用于Tooltip的颜色
 * @returns 随机的深色Tooltip背景色
 */
export function getRandomTooltipColor(): string {
  return generateTooltipColor()
}