<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;

class IndexController extends Controller
{
    const DEFAULT_LANGUAGE = 'bg';

    /**
     * Show the application dashboard.
     *
     * @param Request $request
     * @param string $locale
     * @return \Illuminate\Http\RedirectResponse
     */
    public function index(Request $request, $locale = '')
    {


        if ($locale) {
            App::setLocale($locale);
            return view('content.home');
        }



        return redirect('/' . self::DEFAULT_LANGUAGE, 301);
    }
    /**
     * Show the application dashboard.
     *
     * @param Request $request
     * @param string $locale
     * @return \Illuminate\Http\RedirectResponse
     */

    public function indexLr(Request $request, $locale = '')
    {



        if ($locale) {
            App::setLocale($locale);
            return view('content.home_lr');
        }



        return redirect('/lr/' . self::DEFAULT_LANGUAGE, 301);
    }

    public function indexCr(Request $request, $locale = '')
    {

        if ($locale) {
            App::setLocale($locale);
            return view('content.home_cr');
        }

        return redirect('/prava-na-deteto/' . self::DEFAULT_LANGUAGE, 301);
    }
}
