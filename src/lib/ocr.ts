import { recognizeText } from '@infinitered/react-native-mlkit-text-recognition';

import type { OcrLine } from './parser';

/** Reconoce el texto de la imagen en el propio móvil y devuelve cada línea con su posición. */
export async function readImage(uri: string): Promise<OcrLine[]> {
  const result = await recognizeText(uri);
  return result.blocks.flatMap((block) =>
    block.lines.map((line) => ({
      text: line.text,
      top: line.frame.top,
      bottom: line.frame.bottom,
      left: line.frame.left,
      right: line.frame.right,
    })),
  );
}
