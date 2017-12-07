<#macro page title="Your default title" keywords="" description="">
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="keywords" content="${keywords}">
    <meta name="description" content="${description}">
    <title>${title!""}</title>

    <link rel="stylesheet" href="http://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <!--[if lt IE 9]>
    <script src="http://cdn.bootcss.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="http://cdn.bootcss.com/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
    ${head!""}
</head>
<body>
    ${body!""}
    <script src="http://cdn.bootcss.com/jquery/3.2.1/jquery.min.js"></script>
    <script src="http://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script>
        let ctx = "${springMacroRequestContext.contextPath}";
    </script>
    ${script!""}
</body>
</html>
</#macro>

<@page></@page>