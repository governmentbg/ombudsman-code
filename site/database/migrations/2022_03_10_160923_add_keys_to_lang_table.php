<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddKeysToLangTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('s_lang', function (Blueprint $table) {
            $table->string('S_Lng_key', 20)->comment('Key')->after('S_Lng_name')->nullable();
            $table->boolean('St_id')->comment('Status')->after('S_Lng_key')->nullable();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('s_lang', function (Blueprint $table) {
            //
        });
    }
}
