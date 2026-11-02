param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$token = ''
function Request($method, $path, $payload = $null) {
    $headers = @{}
    if ($script:token) { $headers['Authorization'] = "Bearer $script:token" }
    $params = @{ Uri = "$BaseUrl$path"; Method = $method; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $payload) {
        $params['ContentType'] = 'application/json; charset=utf-8'
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json -Depth 10 -Compress))
    }
    try { return Invoke-RestMethod @params }
    catch {
        Write-Host "FAILED $method $path" -ForegroundColor Red
        if ($_.Exception.Response) {
            Write-Host "HTTP $([int]$_.Exception.Response.StatusCode)"
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host $reader.ReadToEnd()
        }
        throw
    }
}
try {
    $register = Request 'POST' '/api/auth/register' @{
        username="smoke$runId";email="smoke$runId@example.com";password='StrongPassword123!';fullName='Smoke Tester'
    }
    $script:token = $register.accessToken
    $null = Request 'GET' '/api/users/me'
    $cat = Request 'POST' '/api/categories' @{name="Category $runId";description='Smoke test'}
    $ing = Request 'POST' '/api/ingredients' @{name="Ingredient $runId";description='Smoke test'}
    $recipe = Request 'POST' '/api/recipes' @{
        name="Recipe $runId";description='Smoke';cookingTime=30;servings=2;instructions='Test'
        categoryId=$cat.id;ingredients=@(@{ingredientId=$ing.id;quantity=500;unit='gram'})
    }
    $null = Request 'GET' "/api/recipes/$($recipe.id)"
    $null = Request 'PUT' "/api/recipes/$($recipe.id)" @{
        name="Recipe updated $runId";description='Updated';cookingTime=40;servings=3;instructions='Test'
        categoryId=$cat.id;ingredients=@(@{ingredientId=$ing.id;quantity=300;unit='gram'})
    }
    # Comments: GET for guests, create/update for the logged-in author.
    $script:token = ''
    $null = Request 'GET' "/api/recipes/$($recipe.id)/comments"
    $script:token = $register.accessToken
    $comment = Request 'POST' "/api/recipes/$($recipe.id)/comments" @{content='Binh luan kiem thu'}
    $null = Request 'PUT' "/api/recipes/$($recipe.id)/comments/$($comment.id)" @{content='Da cap nhat binh luan'}
    $comments = @(Request 'GET' "/api/recipes/$($recipe.id)/comments")
    if ($comments.Count -lt 1) { throw 'Comment missing after create' }
    # Delete recipe with its comment: confirms referential cleanup.
    $null = Request 'DELETE' "/api/recipes/$($recipe.id)"
    $null = Request 'DELETE' "/api/ingredients/$($ing.id)"
    $null = Request 'DELETE' "/api/categories/$($cat.id)"
    Write-Host "PASS: register, JWT, profile, category, ingredient, recipe CRUD, comments, cleanup" -ForegroundColor Green
} catch { Write-Host 'SMOKE TEST FAILED. Copy the error above.' -ForegroundColor Red; exit 1 }
