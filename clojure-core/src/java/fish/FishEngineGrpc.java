package fish;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class FishEngineGrpc {

  private FishEngineGrpc() {}

  public static final java.lang.String SERVICE_NAME = "fish.FishEngine";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<fish.Fish.ImageRequest,
      fish.Fish.TensorResponse> getGetTensorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTensor",
      requestType = fish.Fish.ImageRequest.class,
      responseType = fish.Fish.TensorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<fish.Fish.ImageRequest,
      fish.Fish.TensorResponse> getGetTensorMethod() {
    io.grpc.MethodDescriptor<fish.Fish.ImageRequest, fish.Fish.TensorResponse> getGetTensorMethod;
    if ((getGetTensorMethod = FishEngineGrpc.getGetTensorMethod) == null) {
      synchronized (FishEngineGrpc.class) {
        if ((getGetTensorMethod = FishEngineGrpc.getGetTensorMethod) == null) {
          FishEngineGrpc.getGetTensorMethod = getGetTensorMethod =
              io.grpc.MethodDescriptor.<fish.Fish.ImageRequest, fish.Fish.TensorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTensor"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  fish.Fish.ImageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  fish.Fish.TensorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new FishEngineMethodDescriptorSupplier("GetTensor"))
              .build();
        }
      }
    }
    return getGetTensorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<fish.Fish.ImageRequest,
      fish.Fish.AnalysisResponse> getAnalyzeImageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AnalyzeImage",
      requestType = fish.Fish.ImageRequest.class,
      responseType = fish.Fish.AnalysisResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<fish.Fish.ImageRequest,
      fish.Fish.AnalysisResponse> getAnalyzeImageMethod() {
    io.grpc.MethodDescriptor<fish.Fish.ImageRequest, fish.Fish.AnalysisResponse> getAnalyzeImageMethod;
    if ((getAnalyzeImageMethod = FishEngineGrpc.getAnalyzeImageMethod) == null) {
      synchronized (FishEngineGrpc.class) {
        if ((getAnalyzeImageMethod = FishEngineGrpc.getAnalyzeImageMethod) == null) {
          FishEngineGrpc.getAnalyzeImageMethod = getAnalyzeImageMethod =
              io.grpc.MethodDescriptor.<fish.Fish.ImageRequest, fish.Fish.AnalysisResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AnalyzeImage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  fish.Fish.ImageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  fish.Fish.AnalysisResponse.getDefaultInstance()))
              .setSchemaDescriptor(new FishEngineMethodDescriptorSupplier("AnalyzeImage"))
              .build();
        }
      }
    }
    return getAnalyzeImageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static FishEngineStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FishEngineStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FishEngineStub>() {
        @java.lang.Override
        public FishEngineStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FishEngineStub(channel, callOptions);
        }
      };
    return FishEngineStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static FishEngineBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FishEngineBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FishEngineBlockingV2Stub>() {
        @java.lang.Override
        public FishEngineBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FishEngineBlockingV2Stub(channel, callOptions);
        }
      };
    return FishEngineBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static FishEngineBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FishEngineBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FishEngineBlockingStub>() {
        @java.lang.Override
        public FishEngineBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FishEngineBlockingStub(channel, callOptions);
        }
      };
    return FishEngineBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static FishEngineFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FishEngineFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FishEngineFutureStub>() {
        @java.lang.Override
        public FishEngineFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FishEngineFutureStub(channel, callOptions);
        }
      };
    return FishEngineFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Returns the raw 5-channel float tensor
     * </pre>
     */
    default void getTensor(fish.Fish.ImageRequest request,
        io.grpc.stub.StreamObserver<fish.Fish.TensorResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTensorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns the analysis metrics (FFT, edges, colors)
     * </pre>
     */
    default void analyzeImage(fish.Fish.ImageRequest request,
        io.grpc.stub.StreamObserver<fish.Fish.AnalysisResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAnalyzeImageMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service FishEngine.
   */
  public static abstract class FishEngineImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return FishEngineGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service FishEngine.
   */
  public static final class FishEngineStub
      extends io.grpc.stub.AbstractAsyncStub<FishEngineStub> {
    private FishEngineStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FishEngineStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FishEngineStub(channel, callOptions);
    }

    /**
     * <pre>
     * Returns the raw 5-channel float tensor
     * </pre>
     */
    public void getTensor(fish.Fish.ImageRequest request,
        io.grpc.stub.StreamObserver<fish.Fish.TensorResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetTensorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns the analysis metrics (FFT, edges, colors)
     * </pre>
     */
    public void analyzeImage(fish.Fish.ImageRequest request,
        io.grpc.stub.StreamObserver<fish.Fish.AnalysisResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAnalyzeImageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service FishEngine.
   */
  public static final class FishEngineBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<FishEngineBlockingV2Stub> {
    private FishEngineBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FishEngineBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FishEngineBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Returns the raw 5-channel float tensor
     * </pre>
     */
    public fish.Fish.TensorResponse getTensor(fish.Fish.ImageRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetTensorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns the analysis metrics (FFT, edges, colors)
     * </pre>
     */
    public fish.Fish.AnalysisResponse analyzeImage(fish.Fish.ImageRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getAnalyzeImageMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service FishEngine.
   */
  public static final class FishEngineBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<FishEngineBlockingStub> {
    private FishEngineBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FishEngineBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FishEngineBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Returns the raw 5-channel float tensor
     * </pre>
     */
    public fish.Fish.TensorResponse getTensor(fish.Fish.ImageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTensorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns the analysis metrics (FFT, edges, colors)
     * </pre>
     */
    public fish.Fish.AnalysisResponse analyzeImage(fish.Fish.ImageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAnalyzeImageMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service FishEngine.
   */
  public static final class FishEngineFutureStub
      extends io.grpc.stub.AbstractFutureStub<FishEngineFutureStub> {
    private FishEngineFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FishEngineFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FishEngineFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Returns the raw 5-channel float tensor
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<fish.Fish.TensorResponse> getTensor(
        fish.Fish.ImageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetTensorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns the analysis metrics (FFT, edges, colors)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<fish.Fish.AnalysisResponse> analyzeImage(
        fish.Fish.ImageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAnalyzeImageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_TENSOR = 0;
  private static final int METHODID_ANALYZE_IMAGE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_TENSOR:
          serviceImpl.getTensor((fish.Fish.ImageRequest) request,
              (io.grpc.stub.StreamObserver<fish.Fish.TensorResponse>) responseObserver);
          break;
        case METHODID_ANALYZE_IMAGE:
          serviceImpl.analyzeImage((fish.Fish.ImageRequest) request,
              (io.grpc.stub.StreamObserver<fish.Fish.AnalysisResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetTensorMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              fish.Fish.ImageRequest,
              fish.Fish.TensorResponse>(
                service, METHODID_GET_TENSOR)))
        .addMethod(
          getAnalyzeImageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              fish.Fish.ImageRequest,
              fish.Fish.AnalysisResponse>(
                service, METHODID_ANALYZE_IMAGE)))
        .build();
  }

  private static abstract class FishEngineBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    FishEngineBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return fish.Fish.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("FishEngine");
    }
  }

  private static final class FishEngineFileDescriptorSupplier
      extends FishEngineBaseDescriptorSupplier {
    FishEngineFileDescriptorSupplier() {}
  }

  private static final class FishEngineMethodDescriptorSupplier
      extends FishEngineBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    FishEngineMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (FishEngineGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new FishEngineFileDescriptorSupplier())
              .addMethod(getGetTensorMethod())
              .addMethod(getAnalyzeImageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
