package com.donabere.amm.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import com.donabere.amm.R
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration


@RunWith(AndroidJUnit4::class)
class MesasActivityTest {

    private lateinit var scenario: ActivityScenario<MesasActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MesasActivity::class.java)

        // Espera breve para que Firebase cargue las mesas y el RecyclerView pinte los items.
        onView(isRoot()).perform(waitFor(2500))
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun alAbrirPantalla_debeMostrarMesas() {
        onView(withContentDescription("mesa_m1"))
            .check(matches(isDisplayed()))

        onView(withContentDescription("mesa_m2"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun unirDosMesasLibres_debeMostrarDialogoYConfirmarAgrupacion() {
        onView(withContentDescription("mesa_m1"))
            .check(matches(isDisplayed()))

        onView(withContentDescription("mesa_m2"))
            .check(matches(isDisplayed()))

        onView(withContentDescription("mesa_m1"))
            .perform(
                GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.CENTER,
                    { view ->
                        val target = view.rootView.findViewWithTag<View>("mesa_m2")
                        val location = IntArray(2)

                        target.getLocationOnScreen(location)

                        floatArrayOf(
                            location[0] + target.width / 2f,
                            location[1] + target.height / 2f
                        )
                    },
                    Press.FINGER
                )
            )

        onView(withText("Agrupar Mesas"))
            .check(matches(isDisplayed()))

        onView(withText("Confirmar"))
            .perform(click())

        // Espera a que Firestore actualice y el RecyclerView repinte
        onView(isRoot()).perform(waitFor(4000))

        // Validación más precisa: la card mesa_m1 ya debe mostrar Mesa 1+2
        onView(withContentDescription("mesa_m1"))
            .check(matches(hasDescendant(withText("Mesa 1+2"))))

        // Y la card mesa_m2 también debe mostrar Mesa 1+2
        onView(withContentDescription("mesa_m2"))
            .check(matches(hasDescendant(withText("Mesa 1+2"))))
    }

    @Test
    fun agregarMesaLibreAMesaOcupada_debeMostrarDialogoDeAsociacion() {
        // Given: Mesa 4 libre y Mesa 3 ocupada
        onView(withContentDescription("mesa_m4"))
            .check(matches(isDisplayed()))

        onView(withContentDescription("mesa_m3"))
            .check(matches(isDisplayed()))

        // Esta validación ayuda a detectar si Firebase está mal preparado
        onView(withContentDescription("mesa_m3"))
            .check(matches(hasDescendant(withText("OCUPADA"))))

        // When: se arrastra Mesa 4 sobre Mesa 3
        onView(withContentDescription("mesa_m4"))
            .perform(
                GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.CENTER,
                    { view ->
                        val target = view.rootView.findViewWithTag<View>("mesa_m3")
                        val location = IntArray(2)

                        target.getLocationOnScreen(location)

                        floatArrayOf(
                            location[0] + target.width / 2f,
                            location[1] + target.height / 2f
                        )
                    },
                    Press.FINGER
                )
            )

        // Then: debe aparecer el diálogo correcto
        onView(withText("Asociar Mesa"))
            .check(matches(isDisplayed()))

        onView(withText("Confirmar"))
            .perform(click())

        // Espera a que Firestore actualice y el RecyclerView repinte
        onView(isRoot()).perform(waitFor(4000))

        // Validación precisa dentro de cada card
        onView(withContentDescription("mesa_m3"))
            .check(matches(hasDescendant(withText("Mesa 3+4"))))

        onView(withContentDescription("mesa_m4"))
            .check(matches(hasDescendant(withText("Mesa 3+4"))))
    }

    @Test
    fun separarMesaAgrupada_debeMostrarDialogoSepararMesa() {
        // Given: Mesa 1 y Mesa 2 están agrupadas visualmente
        onView(withContentDescription("mesa_m1"))
            .check(matches(hasDescendant(withText("Mesa 1+2"))))

        onView(withContentDescription("mesa_m2"))
            .check(matches(hasDescendant(withText("Mesa 1+2"))))

        // When: se mantiene presionada Mesa 1 y se arrastra fuera del grupo
        onView(withContentDescription("mesa_m1"))
            .perform(longPressDragToRecyclerBackground())

        onView(isRoot()).perform(waitFor(1000))

        // Then: aparece el diálogo de separación
        onView(withText("Separar Mesa"))
            .check(matches(isDisplayed()))

        onView(withText("Separar"))
            .perform(click())

        // Espera a que Firestore actualice y el RecyclerView repinte
        onView(isRoot()).perform(waitFor(4000))

        // Then: las mesas vuelven a mostrarse individuales
        onView(withContentDescription("mesa_m1"))
            .check(matches(hasDescendant(withText("Mesa 1"))))

        onView(withContentDescription("mesa_m2"))
            .check(matches(hasDescendant(withText("Mesa 2"))))
    }
    private fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String {
                return "Esperar $millis milisegundos"
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
    private fun longPressDragToRecyclerBackground(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun getDescription(): String {
                return "Hacer long press y arrastrar la mesa hacia el fondo libre del RecyclerView"
            }

            override fun perform(uiController: UiController, view: View) {
                val startLocation = IntArray(2)
                view.getLocationOnScreen(startLocation)

                val startX = startLocation[0] + view.width / 2f
                val startY = startLocation[1] + view.height / 2f

                val recyclerView = view.rootView.findViewById<View>(R.id.rvMesas)
                val recyclerLocation = IntArray(2)
                recyclerView.getLocationOnScreen(recyclerLocation)

                val endX = recyclerLocation[0] + recyclerView.width / 2f
                val endY = recyclerLocation[1] + recyclerView.height - 80f

                val downTime = SystemClock.uptimeMillis()

                val downEvent = MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_DOWN,
                    startX,
                    startY,
                    0
                )
                uiController.injectMotionEvent(downEvent)

                uiController.loopMainThreadForAtLeast(
                    ViewConfiguration.getLongPressTimeout().toLong() + 300
                )

                val moveSteps = 20
                for (i in 1..moveSteps) {
                    val progress = i / moveSteps.toFloat()
                    val moveX = startX + (endX - startX) * progress
                    val moveY = startY + (endY - startY) * progress

                    val moveEvent = MotionEvent.obtain(
                        downTime,
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_MOVE,
                        moveX,
                        moveY,
                        0
                    )

                    uiController.injectMotionEvent(moveEvent)
                    uiController.loopMainThreadForAtLeast(20)
                }

                val upEvent = MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    endX,
                    endY,
                    0
                )
                uiController.injectMotionEvent(upEvent)

                downEvent.recycle()
                upEvent.recycle()

                uiController.loopMainThreadForAtLeast(500)
            }
        }
    }
}