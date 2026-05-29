type ClassInput =
  | string
  | number
  | false
  | null
  | undefined
  | ClassInput[]
  | Record<string, boolean>;

const flattenClassInput = (input: ClassInput, classes: string[]) => {
  if (!input) {
    return;
  }

  if (typeof input === 'string' || typeof input === 'number') {
    classes.push(String(input));
    return;
  }

  if (Array.isArray(input)) {
    input.forEach((item) => flattenClassInput(item, classes));
    return;
  }

  Object.entries(input).forEach(([key, value]) => {
    if (value) {
      classes.push(key);
    }
  });
};

export const cn = (...inputs: ClassInput[]) => {
  const classes: string[] = [];
  inputs.forEach((input) => flattenClassInput(input, classes));
  return classes.join(' ');
};
