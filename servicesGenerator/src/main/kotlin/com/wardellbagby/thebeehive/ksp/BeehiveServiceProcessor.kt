package com.wardellbagby.thebeehive.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val NETWORK_SERVICE_FQN =
  "com.wardellbagby.thebeehive.service.annotations.NetworkService"
private const val GET_FQN = "com.wardellbagby.thebeehive.service.annotations.GET"
private const val POST_FQN = "com.wardellbagby.thebeehive.service.annotations.POST"
private const val WS_FQN = "com.wardellbagby.thebeehive.service.annotations.WS"
private const val BODY_FQN = "com.wardellbagby.thebeehive.service.annotations.Body"
private const val NETWORK_RESPONSE_FQN = "com.wardellbagby.thebeehive.service.NetworkResponse"
private const val EMPTY_RESPONSE_FQN = "com.wardellbagby.thebeehive.service.EmptyResponse"
private const val FLOW_FQN = "kotlinx.coroutines.flow.Flow"

private val HTTP_CLIENT = ClassName("io.ktor.client", "HttpClient")
private val APPLICATION_PLUGIN = ClassName("io.ktor.server.application", "ApplicationPlugin")
private val HTTP_METHOD = ClassName("io.ktor.http", "HttpMethod")
private val CONTENT_TYPE = ClassName("io.ktor.http", "ContentType")
private val FRAME = ClassName("io.ktor.websocket", "Frame")
private val JSON = ClassName("kotlinx.serialization.json", "Json")
private val FLOW = ClassName("kotlinx.coroutines.flow", "Flow")

private val CREATE_APPLICATION_PLUGIN =
  MemberName("io.ktor.server.application", "createApplicationPlugin")
private val CALL_RECEIVE = MemberName("io.ktor.server.request", "receive")
private val REQUEST_HTTP_METHOD = MemberName("io.ktor.server.request", "httpMethod")
private val REQUEST_PATH = MemberName("io.ktor.server.request", "path")
private val SET_BODY = MemberName("io.ktor.client.request", "setBody")
private val CONTENT_TYPE_FN = MemberName("io.ktor.http", "contentType")
private val CLIENT_GET = MemberName("io.ktor.client.request", "get")
private val CLIENT_POST = MemberName("io.ktor.client.request", "post")
private val CLIENT_WEBSOCKET = MemberName("io.ktor.client.plugins.websocket", "webSocket")
private val SERVER_WEBSOCKET = MemberName("io.ktor.server.websockets", "webSocket")
private val ROUTING_FN = MemberName("io.ktor.server.routing", "routing")
private val FLOW_FN = MemberName("kotlinx.coroutines.flow", "flow")
private val PERFORM_NETWORK_CALL =
  MemberName("com.wardellbagby.thebeehive.service", "performNetworkCall")
private val SEND_NETWORK_RESPONSE =
  MemberName("com.wardellbagby.thebeehive.service", "sendNetworkResponse")

class BeehiveServiceProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String> = emptyMap(),
) : SymbolProcessor {

  private val generateMode = options["generate"] ?: "all"

  private var ran = false

  @OptIn(KspExperimental::class)
  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (ran) return emptyList()
    ran = true

    // getSymbolsWithAnnotation only finds symbols in the current module. For modules where
    // @NetworkService interfaces are in a dependency (e.g. networking:core), fall back to
    // scanning all declarations in the service package via getDeclarationsFromPackage, which
    // includes compiled symbols from dependency jars.
    val annotatedInModule =
      resolver.getSymbolsWithAnnotation(NETWORK_SERVICE_FQN).filterIsInstance<KSClassDeclaration>()

    val servicePackages =
      options["servicePackages"]?.split(",") ?: listOf("com.wardellbagby.thebeehive.service")

    val annotatedInDeps =
      servicePackages
        .flatMap { pkg ->
          resolver.getDeclarationsFromPackage(pkg).filterIsInstance<KSClassDeclaration>()
        }
        .filter { it.hasAnnotationFqn(NETWORK_SERVICE_FQN) }

    (annotatedInModule + annotatedInDeps)
      .distinctBy { it.qualifiedName?.asString() }
      .forEach {
        if (generateMode == "client" || generateMode == "all") generateClientClass(it)
        if (generateMode == "server" || generateMode == "all") generateServerPlugin(it)
      }

    return emptyList()
  }

  private fun generateClientClass(templateClass: KSClassDeclaration) {
    val name = templateClass.simpleName.asString()
    val pkg = templateClass.packageName.asString()
    val functions = templateClass.routeFunctions()

    val typeSpec =
      TypeSpec.classBuilder("${name}Client")
        .addSuperinterface(templateClass.toClassName())
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("client", HTTP_CLIENT)
            .addParameter("baseUrl", String::class)
            .build()
        )
        .addProperty(
          PropertySpec.builder("client", HTTP_CLIENT, KModifier.PRIVATE)
            .initializer("client")
            .build()
        )
        .addProperty(
          PropertySpec.builder("baseUrl", String::class, KModifier.PRIVATE)
            .initializer("baseUrl")
            .build()
        )
        .apply { functions.forEach { fn -> addFunction(buildClientFunction(fn)) } }
        .build()

    FileSpec.builder(pkg, "${name}Client")
      .addFileComment("Generated by BeehiveServiceProcessor — do not edit")
      .addType(typeSpec)
      .build()
      .writeTo(
        codeGenerator,
        Dependencies(
          aggregating = false,
          *listOfNotNull(templateClass.containingFile).toTypedArray(),
        ),
      )
  }

  private fun buildClientFunction(fn: KSFunctionDeclaration): FunSpec {
    if (fn.hasAnnotationFqn(WS_FQN)) {
      if (fn.hasWsReturnTypeValidationError()) return buildErrorFunction(fn)
      return buildClientWsFunction(fn)
    }

    if (fn.hasBodyValidationError() || fn.hasReturnTypeValidationError()) {
      return buildErrorFunction(fn)
    }

    val isGet = fn.hasAnnotationFqn(GET_FQN)
    val path = fn.annotationStringArg(if (isGet) GET_FQN else POST_FQN)
    val returnsEmpty = fn.returnsEmptyResponse()
    val bodyParam = fn.bodyParam()
    val returnType = fn.returnType!!.toTypeName()

    val body = buildCodeBlock {
      if (returnsEmpty) {
        beginControlFlow("return %M", PERFORM_NETWORK_CALL)
      } else {
        val innerType = fn.returnType!!.resolve().arguments.first().type!!.toTypeName()
        beginControlFlow("return %M<%T>", PERFORM_NETWORK_CALL, innerType)
      }
      if (isGet) {
        addStatement($$"client.%M(\"$baseUrl%L\")", CLIENT_GET, path)
      } else {
        beginControlFlow($$"client.%M(\"$baseUrl%L\")", CLIENT_POST, path)
        addStatement("%M(%T.Application.Json)", CONTENT_TYPE_FN, CONTENT_TYPE)
        if (bodyParam != null) {
          addStatement("%M(%L)", SET_BODY, bodyParam.name!!.asString())
        }
        endControlFlow()
      }
      endControlFlow()
    }

    return FunSpec.builder(fn.simpleName.asString())
      .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
      .apply {
        fn.parameters.forEach { p ->
          addParameter(p.name!!.asString(), p.type.resolve().toTypeName())
        }
        returns(returnType)
      }
      .addCode(body)
      .build()
  }

  private fun buildClientWsFunction(fn: KSFunctionDeclaration): FunSpec {
    val path = fn.annotationStringArg(WS_FQN)
    val innerType = fn.returnType!!.resolve().arguments.first().type!!.toTypeName()
    val returnType = FLOW.parameterizedBy(innerType)

    val body = buildCodeBlock {
      beginControlFlow("return %M", FLOW_FN)
      beginControlFlow($$"client.%M(\"$baseUrl%L\")", CLIENT_WEBSOCKET, path)
      beginControlFlow("for (frame in incoming)")
      beginControlFlow("if (frame is %T.Text)", FRAME)
      addStatement("emit(%T.decodeFromString<%T>(frame.readText()))", JSON, innerType)
      endControlFlow()
      endControlFlow()
      endControlFlow()
      endControlFlow()
    }

    return FunSpec.builder(fn.simpleName.asString())
      .addModifiers(KModifier.OVERRIDE)
      .returns(returnType)
      .addCode(body)
      .build()
  }

  private fun generateServerPlugin(templateClass: KSClassDeclaration) {
    val name = templateClass.simpleName.asString()
    val pkg = templateClass.packageName.asString()
    val fullClassName = templateClass.toClassName()
    val functions = templateClass.routeFunctions()
    val httpFunctions = functions.filter { !it.hasAnnotationFqn(WS_FQN) }
    val wsFunctions = functions.filter { it.hasAnnotationFqn(WS_FQN) }

    val pluginBody = buildCodeBlock {
      beginControlFlow("return %M(%S)", CREATE_APPLICATION_PLUGIN, "${name}Routes")
      if (httpFunctions.isNotEmpty()) {
        add("onCall { call ->\n")
        indent()
        beginControlFlow("when")
        httpFunctions.forEach { fn -> addServerWhenBranch(fn) }
        endControlFlow()
        unindent()
        add("}\n")
      }
      if (wsFunctions.isNotEmpty()) {
        beginControlFlow("application.%M", ROUTING_FN)
        wsFunctions.forEach { fn -> addServerWebSocketRoute(fn) }
        endControlFlow()
      }
      endControlFlow()
    }

    val funSpec =
      FunSpec.builder("${name}ServerPlugin")
        .addParameter("impl", fullClassName)
        .returns(APPLICATION_PLUGIN.parameterizedBy(Unit::class.asClassName()))
        .addCode(pluginBody)
        .build()

    FileSpec.builder("$pkg.server", "${name}ServerPlugin")
      .addFileComment("Generated by BeehiveServiceProcessor — do not edit")
      .addFunction(funSpec)
      .build()
      .writeTo(codeGenerator, Dependencies(aggregating = false))
  }

  private fun com.squareup.kotlinpoet.CodeBlock.Builder.addServerWhenBranch(
    fn: KSFunctionDeclaration
  ) {
    val isGet = fn.hasAnnotationFqn(GET_FQN)
    val path = fn.annotationStringArg(if (isGet) GET_FQN else POST_FQN)
    val httpMethodName = if (isGet) "Get" else "Post"
    fn.hasBodyValidationError()
    fn.hasReturnTypeValidationError()
    val bodyParam = fn.bodyParam()
    val callName = fn.simpleName.asString()

    beginControlFlow(
      "%T.%L == call.request.%M && %S == call.request.%M() ->",
      HTTP_METHOD,
      httpMethodName,
      REQUEST_HTTP_METHOD,
      path,
      REQUEST_PATH,
    )
    if (bodyParam != null) {
      val bodyTypeName = bodyParam.type.resolve().toTypeName()
      addStatement("val body = call.%M<%T>()", CALL_RECEIVE, bodyTypeName)
      addStatement("call.%M(impl.%L(body))", SEND_NETWORK_RESPONSE, callName)
    } else {
      addStatement("call.%M(impl.%L())", SEND_NETWORK_RESPONSE, callName)
    }
    endControlFlow()
  }

  private fun com.squareup.kotlinpoet.CodeBlock.Builder.addServerWebSocketRoute(
    fn: KSFunctionDeclaration
  ) {
    fn.hasWsReturnTypeValidationError()
    val path = fn.annotationStringArg(WS_FQN)
    val callName = fn.simpleName.asString()
    val innerType = fn.returnType!!.resolve().arguments.first().type!!.toTypeName()

    beginControlFlow("%M(%S)", SERVER_WEBSOCKET, path)
    beginControlFlow("impl.%L().collect { item ->", callName)
    addStatement("send(%T.Text(%T.encodeToString<%T>(item)))", FRAME, JSON, innerType)
    endControlFlow()
    endControlFlow()
  }

  private fun KSClassDeclaration.routeFunctions(): List<KSFunctionDeclaration> =
    getAllFunctions()
      .filter { fn ->
        fn.hasAnnotationFqn(GET_FQN) || fn.hasAnnotationFqn(POST_FQN) || fn.hasAnnotationFqn(WS_FQN)
      }
      .toList()

  private fun KSAnnotated.hasAnnotationFqn(fqn: String): Boolean = annotations.any { ann ->
    ann.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
  }

  private fun KSFunctionDeclaration.annotationStringArg(fqn: String): String =
    annotations
      .first { ann -> ann.annotationType.resolve().declaration.qualifiedName?.asString() == fqn }
      .arguments
      .first { it.name?.asString() == "value" }
      .value as String

  private fun KSValueParameter.hasAnnotationFqn(fqn: String): Boolean = annotations.any { ann ->
    ann.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
  }

  private fun KSFunctionDeclaration.returnsEmptyResponse(): Boolean {
    val resolved = returnType?.resolve() ?: return false
    val fqn = resolved.declaration.qualifiedName?.asString()
    if (fqn == EMPTY_RESPONSE_FQN) return true
    if (fqn != NETWORK_RESPONSE_FQN) return false
    val innerFqn =
      resolved.arguments.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString()
    return innerFqn == "kotlin.Unit"
  }

  private fun KSFunctionDeclaration.hasReturnTypeValidationError(): Boolean {
    val fqn = returnType?.resolve()?.declaration?.qualifiedName?.asString()
    if (fqn != NETWORK_RESPONSE_FQN && fqn != EMPTY_RESPONSE_FQN) {
      logger.error(
        "@GET/@POST function '${simpleName.asString()}' must return NetworkResponse<T> or EmptyResponse",
        this,
      )
      return true
    }
    return false
  }

  private fun KSFunctionDeclaration.hasWsReturnTypeValidationError(): Boolean {
    val fqn = returnType?.resolve()?.declaration?.qualifiedName?.asString()
    if (fqn != FLOW_FQN) {
      logger.error("@WS function '${simpleName.asString()}' must return Flow<T>", this)
      return true
    }
    return false
  }

  private fun KSFunctionDeclaration.hasBodyValidationError(): Boolean {
    val isGet = hasAnnotationFqn(GET_FQN)
    val bodyParams = parameters.filter { it.hasAnnotationFqn(BODY_FQN) }
    val name = simpleName.asString()

    if (isGet && bodyParams.isNotEmpty()) {
      logger.error("@GET function '$name' must not have @Body parameters", this)
      return true
    }
    if (!isGet) {
      if (parameters.isNotEmpty() && bodyParams.isEmpty()) {
        logger.error("@POST function '$name' has parameters but none annotated with @Body", this)
        return true
      }
      if (bodyParams.size > 1) {
        logger.error("@POST function '$name' has more than one @Body parameter", this)
        return true
      }
    }
    return false
  }

  private fun KSFunctionDeclaration.bodyParam(): KSValueParameter? = parameters.firstOrNull {
    it.hasAnnotationFqn(BODY_FQN)
  }

  private fun buildErrorFunction(fn: KSFunctionDeclaration): FunSpec {
    val returnType =
      fn.returnType?.toTypeName()
        ?: ClassName("com.wardellbagby.thebeehive.service", "NetworkResponse")
          .parameterizedBy(Unit::class.asClassName())
    return FunSpec.builder(fn.simpleName.asString())
      .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
      .apply {
        fn.parameters.forEach { p ->
          addParameter(p.name!!.asString(), p.type.resolve().toTypeName())
        }
        returns(returnType)
      }
      .addStatement("error(%S)", "Code generation failed for ${fn.simpleName.asString()}")
      .build()
  }
}
