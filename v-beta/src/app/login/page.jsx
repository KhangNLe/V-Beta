import { LoginForm } from "@/components/ui/login-form"
import { AuthBrandedShell } from "@/components/auth-branded-shell"

export default function LoginPage() {
  return (
    <AuthBrandedShell>
      <LoginForm />
    </AuthBrandedShell>
  )
}
