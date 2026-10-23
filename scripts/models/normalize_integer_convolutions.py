#!/usr/bin/env python3
"""Normalize constant ConvInteger weights for ORT's macOS Intel u8/u8 kernel.

Developer tool only: pip install onnx==1.20.1. No model download or retraining.
Subtracting the shifted zero point preserves every quantized weight exactly:
(w + 128) - (z + 128) == w - z. MatMulInteger weights are left unchanged.
"""
import argparse
from pathlib import Path
import numpy as np
import onnx
from onnx import TensorProto, numpy_helper


def normalize(model):
    initializers = {tensor.name: tensor for tensor in model.graph.initializer}
    count = 0
    for node in model.graph.node:
        if node.op_type != "ConvInteger" or node.domain:
            continue
        weight = initializers.get(node.input[1])
        if weight is None or weight.data_type != TensorProto.INT8:
            continue
        if len(node.input) < 4 or node.input[3] not in initializers:
            raise ValueError(f"{node.name}: expected a constant weight zero point")
        zero = initializers[node.input[3]]
        if zero.data_type != TensorProto.INT8:
            raise ValueError(f"{node.name}: weight and zero point types differ")
        # Clone initializers so consumers outside this convolution keep their original types.
        for slot, tensor in ((1, weight), (3, zero)):
            name = tensor.name + "_conv_uint8"
            if name in initializers:
                raise ValueError(f"Initializer collision: {name}")
            original = numpy_helper.to_array(tensor).astype(np.int16)
            shifted = (original + 128).astype(np.uint8)
            assert np.array_equal(shifted.astype(np.int16) - 128, original)
            replacement = numpy_helper.from_array(shifted, name)
            model.graph.initializer.append(replacement)
            initializers[name] = replacement
            node.input[slot] = name
        count += 1
    used = {name for node in model.graph.node for name in node.input}
    used.update(output.name for output in model.graph.output)
    kept = [tensor for tensor in model.graph.initializer if tensor.name in used]
    del model.graph.initializer[:]
    model.graph.initializer.extend(kept)
    onnx.checker.check_model(model)
    return count


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("model", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    model = onnx.load(args.model)
    count = normalize(model)
    onnx.save(model, args.output)
    print(f"Normalized {count} integer convolutions: {args.output}")
