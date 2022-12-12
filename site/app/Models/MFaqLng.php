<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $FqL_id
 * @property int    $Fq_id
 * @property int    $S_Lng_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $FqL_path
 * @property string $FqL_title
 * @property string $FqL_body
 * @property string $FqL_meta
 */
class MFaqLng extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_faq_lng';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'FqL_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Fq_id', 'S_Lng_id', 'FqL_path', 'FqL_title', 'FqL_body', 'FqL_meta', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'FqL_id' => 'int', 'Fq_id' => 'int', 'S_Lng_id' => 'int', 'FqL_path' => 'string', 'FqL_title' => 'string', 'FqL_body' => 'string', 'FqL_meta' => 'string', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();


        static::creating(function ($article) {
            if ($article->S_Lng_id == 2) {
                $article->FqL_path =  Str::slug(C2L($article->FqL_title));
            }
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {


            $truncated = substr($article->FqL_title, 0, 80);
            $article->FqL_path = Str::slug(C2L($truncated));
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }
    public function eq_faq()
    {
        return $this->belongsTo(MFaq::class, 'Fq_id');
    }
}
